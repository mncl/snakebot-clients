package se.cygni.snake;

import se.cygni.snake.client.MapCoordinate;

import java.util.Map;

public class RecMapTools {

    private RecMapTools(){/*non-instance*/}

    interface RecMap {
        <R> R see(MapCoordinate coordinate, VisitTile<R> visitor);
    }

    interface VisitTile<R> {
        R obstacle();

        R food();

        R snakeBody();

        R snakeHead(String playerId);

        R snakeTail();

        R empty();
    }

    public static class PartiallyUpdatedRecMap implements RecMap {
        private final Map<MapCoordinate, SimpleSnakePlayer.TileMemory> memoryMap;
        private final RecMap fallback;

        public static PartiallyUpdatedRecMap from(Map<MapCoordinate, SimpleSnakePlayer.TileMemory> memoryMap, RecMap fallback) {
            return new PartiallyUpdatedRecMap(memoryMap, fallback);
        }

        private PartiallyUpdatedRecMap(Map<MapCoordinate, SimpleSnakePlayer.TileMemory> memoryMap, RecMap fallback) {
            this.memoryMap = memoryMap;
            this.fallback = fallback;
        }

        @Override
        public <R> R see(MapCoordinate coordinate, VisitTile<R> visitor) {
            SimpleSnakePlayer.TileMemory tileMemory = memoryMap.get(coordinate);
            if (tileMemory == null) {
                return fallback.see(coordinate, visitor);
            }
            return tileMemory.see(visitor);
        }
    }


}
