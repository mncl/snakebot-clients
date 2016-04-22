package se.cygni.snake;

class IsHead implements RecMapTools.VisitTile<Boolean> {
    private final String notThisHead;

    public IsHead(String notThisHead) {
        this.notThisHead = notThisHead;
    }

    @Override
    public Boolean obstacle() {
        return false;
    }

    @Override
    public Boolean food() {
        return false;
    }

    @Override
    public Boolean snakeBody() {
        return false;
    }

    @Override
    public Boolean snakeHead(String id) {
        return id.equals(notThisHead) ? false : true;
    }

    @Override
    public Boolean snakeTail() {
        return false;
    }

    @Override
    public Boolean empty() {
        return false;
    }
}
