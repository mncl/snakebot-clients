package se.cygni.snake;

import se.cygni.snake.api.model.SnakeDirection;

class ScoreDepthImpl implements SimpleSnakePlayer.ScoreDepth {

    private final double score;
    private final int depth;
    private final SnakeDirection direction;

    public ScoreDepthImpl(double score, int depth, SnakeDirection direction) {
        this.score = score;
        this.depth = depth;
        this.direction = direction;
    }

    @Override
    public <R> R scoreDepth(SimpleSnakePlayer.ScoredDepthVisitor<R> visitor) {
        return visitor.score(score, depth, direction);
    }
}
