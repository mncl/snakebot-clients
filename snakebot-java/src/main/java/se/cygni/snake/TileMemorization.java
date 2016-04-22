package se.cygni.snake;


import se.cygni.snake.RecMapTools.VisitTile;
import se.cygni.snake.SimpleSnakePlayer.TileMemory;

public class TileMemorization implements VisitTile<SimpleSnakePlayer.TileMemory> {

    @Override
    public TileMemory obstacle() {
        return new TileMemory() {
            @Override
            public <R> R see(VisitTile<R> visitor) {
                return visitor.obstacle();
            }
        };
    }

    @Override
    public TileMemory food() {
        return new TileMemory() {
            @Override
            public <R> R see(VisitTile<R> visitor) {
                return visitor.food();
            }
        };
    }

    @Override
    public TileMemory snakeBody() {
        return new TileMemory() {
            @Override
            public <R> R see(VisitTile<R> visitor) {
                return visitor.snakeBody();
            }
        };
    }

    @Override
    public TileMemory snakeHead(String id) {
        return new TileMemory() {
            @Override
            public <R> R see(VisitTile<R> visitor) {
                return visitor.snakeHead(id);
            }
        };
    }

    @Override
    public TileMemory snakeTail() {
        return new TileMemory() {
            @Override
            public <R> R see(VisitTile<R> visitor) {
                return visitor.snakeTail();
            }
        };
    }

    @Override
    public TileMemory empty() {
        return new TileMemory() {
            @Override
            public <R> R see(VisitTile<R> visitor) {
                return visitor.empty();
            }
        };
    }
}
