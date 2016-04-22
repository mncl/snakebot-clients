package se.cygni.snake;



class IsPassable implements RecMapTools.VisitTile<Boolean> {
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
    public Boolean snakeHead(String id) {
        return false;
    }

    @Override
    public Boolean snakeTail() {
        return false;
    }

    @Override
    public Boolean empty() {
        return true;
    }
}
