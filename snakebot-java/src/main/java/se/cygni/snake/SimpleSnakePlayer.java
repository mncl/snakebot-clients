package se.cygni.snake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.cygni.snake.api.event.GameEndedEvent;
import se.cygni.snake.api.event.GameStartingEvent;
import se.cygni.snake.api.event.MapUpdateEvent;
import se.cygni.snake.api.event.SnakeDeadEvent;
import se.cygni.snake.api.exception.InvalidPlayerName;
import se.cygni.snake.api.model.GameMode;
import se.cygni.snake.api.model.GameSettings;
import se.cygni.snake.api.model.MapEmpty;
import se.cygni.snake.api.model.MapFood;
import se.cygni.snake.api.model.MapObstacle;
import se.cygni.snake.api.model.MapSnakeBody;
import se.cygni.snake.api.model.MapSnakeHead;
import se.cygni.snake.api.model.SnakeDirection;
import se.cygni.snake.api.model.TileContent;
import se.cygni.snake.api.response.PlayerRegistered;
import se.cygni.snake.client.AnsiPrinter;
import se.cygni.snake.client.BaseSnakeClient;
import se.cygni.snake.client.MapCoordinate;
import se.cygni.snake.client.MapUtil;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import se.cygni.snake.RecMapTools.RecMap;
import se.cygni.snake.RecMapTools.VisitTile;
import se.cygni.snake.RecMapTools.PartiallyUpdatedRecMap;


public class SimpleSnakePlayer extends BaseSnakeClient {

    private static Logger log = LoggerFactory.getLogger(SimpleSnakePlayer.class);

    private final AnsiPrinter ansiPrinter;
    private final String hostname;
    private final int port;
    private final String playerName;


    static final AtomicInteger playerId = new AtomicInteger(0);

    public static SimpleSnakePlayer localConnection() {
        return new SimpleSnakePlayer(new AnsiPrinter(), "127.0.0.1", 8080, "local" + playerId.getAndIncrement());
    }

    private SimpleSnakePlayer(AnsiPrinter ansiPrinter, String hostname, int port, String playerName) {
        this.ansiPrinter = ansiPrinter;
        this.hostname = hostname;
        this.port = port;
        this.playerName = playerName;
    }

    public static void main(String[] args) {

        Runnable task = () -> {

            SimpleSnakePlayer sp = localConnection();
            sp.connect();

            // Keep this process alive as long as the
            // Snake is connected and playing.
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (sp.isPlaying());

            log.info("Shutting down");
        };


        Thread thread = new Thread(task);
        thread.start();
    }


    public RecMap from(MapUtil mu, String playerName) {
        return new RecMap() {

            @Override
            public <R> R see(MapCoordinate coordinate, RecMapTools.VisitTile<R> visitor) {
                if (mu.isCoordinateOutOfBounds(coordinate)) {
                    return visitor.obstacle();
                }

                TileContent tileAt = mu.getTileAt(coordinate);
                if (tileAt instanceof MapObstacle) {
                    return visitor.obstacle();
                }

                if (tileAt instanceof MapFood) {
                    return visitor.food();
                }

                if (tileAt instanceof MapSnakeBody) {
                    MapSnakeBody body = (MapSnakeBody) tileAt;

                    if (body.isTail() && !body.getPlayerId().equals(playerName)) {
                        return visitor.snakeTail();
                    }
                    return visitor.snakeBody();
                }

                if (tileAt instanceof MapSnakeHead) {
                    MapSnakeHead head = (MapSnakeHead) tileAt;
                    String playerId = head.getPlayerId();
                    if (playerId.equals(playerName)) {
                        return visitor.snakeBody();
                    }
                    return visitor.snakeHead(playerId);
                }

                if (tileAt instanceof MapEmpty) {
                    return visitor.empty();
                }

                throw new RuntimeException("unexpectedMapSquareType");
            }
        };
    }


    public static VisitTile<Double> basicScoreing(Double randomSpawnChance) {
        return new VisitTile<Double>() {
            @Override
            public Double obstacle() {
                return -1000.0;
            }

            @Override
            public Double food() {
                return +2.0;
            }

            @Override
            public Double snakeBody() {
                return -999.0;
            }

            @Override
            public Double snakeHead(String playerId) {
                return -3000.0;
            }

            @Override
            public Double snakeTail() {
                return +4.0;
            }

            @Override
            public Double empty() {
                return randomSpawnChance;
            }
        };
    }


    public static Map<MapCoordinate, SnakeDirection> movementDirections(MapCoordinate coord) {
        return MapBuiling
                .map(coord.translateBy(0, -1), SnakeDirection.UP)
                .put(coord.translateBy(0, +1), SnakeDirection.DOWN)
                .put(coord.translateBy(-1, 0), SnakeDirection.LEFT)
                .put(coord.translateBy(+1, 0), SnakeDirection.RIGHT).build();
    }


    public static void lookAroundNShallowFreeDepth(RecMap map, MapCoordinate coord, Consumer<ScoreDepth> scoreoutput, String ownId) {
        RecMap otherSnakesMoveFirst = expandHeads(map, ownId);
        RecMapGrowing recMapGrowing = growingHeadsPreRenderd(map, otherSnakesMoveFirst, coord, ownId);

        RecMapTools.VisitTile<Double> scoreFunction = basicScoreing(0.1);
        Map<MapCoordinate, SnakeDirection> dix = movementDirections(coord);

        Map<SnakeDirection, Double> scoredDirections = MapBuiling.modMapValues(MapBuiling.mapSwap(dix), (snakeDirection, coordinate) -> map.see(coordinate, scoreFunction));
        scoredDirections.entrySet().forEach(entry -> scoreoutput.accept(new ScoreDepthImpl(entry.getValue(), 0, entry.getKey())));

        Map<MapCoordinate, SnakeDirection> openNodes = new HashMap<>();
        Map<MapCoordinate, SnakeDirection> scanningNodes = new HashMap<>();
        Map<MapCoordinate, Double> nodeScores = new HashMap<>();

        openNodes.putAll(dix);

        int depthCounter = 0;
        while (scanningNodes.size() + openNodes.size() > 0) {
            for (Map.Entry<MapCoordinate, SnakeDirection> entry : scanningNodes.entrySet()) {
                SnakeDirection currentDirection = entry.getValue();
                MapCoordinate key = entry.getKey();
                int currentDepth = depthCounter;
                Double existingScore = nodeScores.getOrDefault(key, 0.0);
                Map<MapCoordinate, Double> newNodes = nextLevel(key, existingScore, scoreFunction, recMapGrowing, new Consumer<Double>() {
                    @Override
                    public void accept(Double score) {
                        scoreoutput.accept(new ScoreDepthImpl(score, currentDepth, currentDirection));
                    }
                });
                Set<Map.Entry<MapCoordinate, Double>> entries = newNodes.entrySet();

                for (Map.Entry<MapCoordinate, Double> newNode : entries) {
                    MapCoordinate newCoordinate = newNode.getKey();
                    openNodes.put(newCoordinate, currentDirection);
                    nodeScores.put(newCoordinate, newNode.getValue());
                }

                if (Thread.interrupted()) {
                    return;
                }
            }
            recMapGrowing = recMapGrowing.get();// look deeper
            Map<MapCoordinate, SnakeDirection> swap = openNodes;
            openNodes = scanningNodes;
            scanningNodes = swap;
            openNodes.clear();
            ++depthCounter;
        }

    }


    private static Map<MapCoordinate, Double> nextLevel(MapCoordinate coordinate, Double previousScore, VisitTile<Double> scoreFunction, RecMapGrowing recMapGrowing, Consumer<Double> submitScore) {

        Double score = recMapGrowing.see(coordinate, scoreFunction) + previousScore * 1.01d;
        submitScore.accept(score);

        if (score < 0) {
            return Collections.emptyMap();
        }
        return MapBuiling.modMapValues(movementDirections(coordinate), (k, v) -> score);
    }


    interface RecMapGrowing extends RecMap, Supplier<RecMapGrowing> {

    }


    interface TileMemory {
        <R> R see(RecMapTools.VisitTile<R> visitor);
    }


    public static RecMapGrowing growingHeadsPreRenderd(RecMap fallback, RecMap map, MapCoordinate ownHead, String ownPlayerId) {

        RecMap expanded = expandHeads(map, "");
        RecMap preRendered = preRenderSubsection(fallback, expanded, ownHead, new VisitTile<Boolean>() {
            @Override
            public Boolean obstacle() {
                return false;
            }

            @Override
            public Boolean food() {
                return true;
            }

            @Override
            public Boolean snakeBody() {
                return false;
            }

            @Override
            public Boolean snakeHead(String playerId) {
                return playerId.equals(ownPlayerId); // we scan past our selfs
            }

            @Override
            public Boolean snakeTail() {
                return false;
            }

            @Override
            public Boolean empty() {
                return null;
            }
        });

        return new RecMapGrowing() {
            @Override
            public RecMapGrowing get() {
                return growingHeadsPreRenderd(fallback, preRendered, ownHead, ownPlayerId);
            }

            @Override
            public <R> R see(MapCoordinate coordinate, RecMapTools.VisitTile<R> visitor) {
                return preRendered.see(coordinate, visitor);
            }
        };

    }

    static public RecMap preRenderSubsection(RecMap fallback, RecMap map, MapCoordinate startingCell, RecMapTools.VisitTile<Boolean> scanAround) {
        TileMemorization cellMemory = new TileMemorization();
        Map<MapCoordinate, TileMemory> memoryMap = renderPartialMap(map, startingCell, scanAround, cellMemory);
        return PartiallyUpdatedRecMap.from(memoryMap, fallback);
    }

    private static Map<MapCoordinate, TileMemory> renderPartialMap(RecMap map, MapCoordinate startingCell, RecMapTools.VisitTile<Boolean> scanAround, TileMemorization cellMemory) {
        Set<MapCoordinate> openList = new HashSet<>();
        Set<MapCoordinate> newNodes = new HashSet<>();
        openList.add(startingCell);

        Map<MapCoordinate, TileMemory> memoryMap = new HashMap<>();

        while (openList.size() > 0) {
            newNodes.clear();
            for (MapCoordinate current : openList) {
                Boolean see = map.see(current, scanAround);
                if (see) {
                    newNodes.addAll(movementDirections(current).keySet());
                }
                memoryMap.put(current, map.see(current, cellMemory));
            }
            openList.clear();
            openList.addAll(newNodes);
            openList.removeAll(memoryMap.keySet());
        }
        return memoryMap;
    }

    public static RecMap expandHeads(RecMap map, String doNotExpandThisSnake) {
        IsHead isHead = new IsHead(doNotExpandThisSnake);
        return new RecMap() {
            private <R> R lookAround(MapCoordinate coordinate, RecMapTools.VisitTile<R> visitor) {
                for (MapCoordinate mc : movementDirections(coordinate).keySet()) {
                    if (map.see(mc, isHead)) {
                        return map.see(mc, visitor);
                    }
                }
                return map.see(coordinate, visitor);
            }

            @Override
            public <R> R see(MapCoordinate coordinate, RecMapTools.VisitTile<R> visitor) {
                return map.see(coordinate, new RecMapTools.VisitTile<R>() {
                    @Override
                    public R obstacle() {
                        return visitor.obstacle();
                    }

                    @Override
                    public R food() {
                        return lookAround(coordinate, visitor);
                    }

                    @Override
                    public R snakeBody() {
                        return visitor.snakeBody();
                    }

                    @Override
                    public R snakeHead(String playerId) {
                        return visitor.snakeHead(playerId);
                    }

                    @Override
                    public R snakeTail() {
                        return visitor.snakeTail();
                    }

                    @Override
                    public R empty() {
                        return lookAround(coordinate, visitor);
                    }
                });
            }
        };
    }

    interface Scored<T> extends Supplier<T> {
        Double score();
    }

    interface ScoredDepthVisitor<R> {
        R score(Double score, int depth, SnakeDirection direction);
    }

    interface ScoreDepth {
        <R> R scoreDepth(ScoredDepthVisitor<R> visitor);
    }


    @Override
    public void onMapUpdate(MapUpdateEvent mapUpdateEvent) {
        ansiPrinter.printMap(mapUpdateEvent);
        ScoreDepthImpl initalScore = new ScoreDepthImpl(Double.MIN_VALUE, -1, SnakeDirection.DOWN);

        Function<ScoreDepth, SnakeDirection> scoreDirection = scoreDepth -> scoreDepth.scoreDepth((score, depth, direction) -> direction);

        BucketMerge<ScoreDepth, SnakeDirection> bucketMerger = BucketMerge.fromSegmentDefaultMerge(
                scoreDirection,
                (SnakeDirection snakeDirection) -> new ScoreDepthImpl(Double.MIN_VALUE, -1, snakeDirection),
                new DepthBeforeScoreMerge());

        String localPlayer = getPlayerId();

        Thread forked = new Thread(() -> {
            se.cygni.snake.api.model.Map map = mapUpdateEvent.getMap();
            MapUtil mapUtil = new MapUtil(map, localPlayer);
            RecMap recMap = from(mapUtil, getPlayerId());
            MapCoordinate myPosition = mapUtil.getMyPosition();
            lookAroundNShallowFreeDepth(recMap, myPosition, bucketMerger, localPlayer);
        });
        forked.start();

        try {
            forked.join(20);
            forked.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ScoreDepth winnignScore = initalScore;
        for (SnakeDirection sd : SnakeDirection.values()) {
            ScoreDepth apply = bucketMerger.apply(sd);
            winnignScore = apply.scoreDepth((score, depth, direction) -> score) > winnignScore.scoreDepth((score, depth, direction) -> score) ? apply : winnignScore;
            System.out.println(apply.scoreDepth(new ScoredDepthVisitor<String>() {
                @Override
                public String score(Double score, int depth, SnakeDirection direction) {
                    return direction.name() + " recived " + score + " points at depth " + depth;
                }
            }));
        }
        SnakeDirection selectedDirection = scoreDirection.apply(winnignScore);

        System.out.println(mapUpdateEvent.getGameTick() + " -> " + selectedDirection.name());

        // Register action here!
        registerMove(mapUpdateEvent.getGameTick(), selectedDirection);
    }


    @Override
    public void onInvalidPlayerName(InvalidPlayerName invalidPlayerName) {

    }

    @Override
    public void onSnakeDead(SnakeDeadEvent snakeDeadEvent) {
        log.info("A snake {} died by {}",
                snakeDeadEvent.getPlayerId(),
                snakeDeadEvent.getDeathReason());
    }

    @Override
    public void onGameEnded(GameEndedEvent gameEndedEvent) {
        log.debug("GameEndedEvent: " + gameEndedEvent);
    }

    @Override
    public void onGameStarting(GameStartingEvent gameStartingEvent) {
        log.debug("GameStartingEvent: " + gameStartingEvent);
    }

    @Override
    public void onPlayerRegistered(PlayerRegistered playerRegistered) {
        log.info("PlayerRegistered: " + playerRegistered);

        // Disable this if you want to start the game manually fromSegmentDefaultMerge
        // the web GUI
        startGame();
    }

    @Override
    public void onSessionClosed() {
        log.info("Session closed");
    }

    @Override
    public void onConnected() {
        log.info("Connected, registering for training...");
        GameSettings gameSettings = new GameSettings.GameSettingsBuilder()
                .withWidth(40)
                .withHeight(40)
                .withMaxNoofPlayers(2)
                .withTimeInMsPerTick(10000000)
                .build();

        registerForGame(gameSettings);
    }

    @Override
    public String getName() {
        return playerName;
    }

    @Override
    public String getServerHost() {
        return hostname;
    }

    @Override
    public int getServerPort() {
        return port;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.training;
    }


    private static class DepthBeforeScoreMerge implements BinaryOperator<ScoreDepth> {
        @Override
        public ScoreDepth apply(ScoreDepth scoreDepth1, ScoreDepth scoreDepth2) {
            return scoreDepth1.scoreDepth((score1, depth1, direction1) -> scoreDepth2.scoreDepth((score2, depth2, direction2) -> {
                if (depth1 == depth2) {
                    return score1 > score2 ? scoreDepth1 : scoreDepth2;
                }
                return depth1 > depth2 ? scoreDepth1 : scoreDepth2;
            }));
        }
    }

}
