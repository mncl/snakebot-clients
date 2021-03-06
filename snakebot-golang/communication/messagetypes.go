package communication

//inbound messages
const GameEnded string = "se.cygni.snake.api.event.GameEndedEvent"
const MapUpdated string = "se.cygni.snake.api.event.MapUpdateEvent"
const SnakeDead string = "se.cygni.snake.api.event.SnakeDeadEvent"
const GameStarting string = "se.cygni.snake.api.event.GameStartingEvent"
const PlayerRegistered string = "se.cygni.snake.api.response.PlayerRegistered"
const InvalidPlayerName string = "se.cygni.snake.api.exception.InvalidPlayerName"

//outbound messages
const RegisterPlayerMessageType string = "se.cygni.snake.api.request.RegisterPlayer"
const StartGame string = "se.cygni.snake.api.request.StartGame"
const RegisterMove string = "se.cygni.snake.api.request.RegisterMove"

//Outbound messages
type gameSettings struct {
	Width                    int  `json:"width"`
	Height                   int  `json:"height"`
	MaxNoofPlayers           int  `json:"maxNoofPlayers"`
	StartSnakeLength         int  `json:"startSnakeLength"`
	TimeInMsPerTick          int  `json:"timeInMsPerTick"`
	ObstaclesEnabled         bool `json:"obstaclesEnabled"`
	FoodEnabled              bool `json:"foodEnabled"`
	EdgeWrapsAround          bool `json:"edgeWrapsAround"`
	HeadToTailConsumes       bool `json:"headToTailConsumes"`
	TailConsumeGrows         bool `json:"tailConsumeGrows"`
	AddFoodLikelihood        int  `json:"addFoodLikelihood"`
	RemoveFoodLikelihood     int  `json:"removeFoodLikelihood"`
	AddObstacleLikelihood    int  `json:"addObstacleLikelihood"`
	RemoveObstacleLikelihood int  `json:"removeObstacleLikelihood"`
}

type gameMessage struct {
	ReceivingPlayerId string `json:"receivingPlayerId"`
	Type              string `json:"type"`
}

type playerRegistrationMessage struct {
	gameMessage
	PlayerName   string       `json:"playerName"`
	Color        string       `json:"color"`
	GameSettings gameSettings `json:"gameSettings"`
}

type registerMoveMessage struct {
	gameMessage
	Direction string `json:"direction"`
}

type startGameMessage struct {
	gameMessage
}

//Inbound messages
type PlayerRegisteredMessage struct {
	gameMessage
	Name         string       `json:"name"`
	Color        string       `json:"color"`
	GameId       string       `json:"gameId"`
	GameSettings gameSettings `json:"gameSettings"`
	GameMode     string       `json:"gameMode"`
}

type MapUpdatedMessage struct {
	Map      Map `json:"map"`
	GameTick int `json:"gameTick"`
}

type InvalidPlayerNameMessage struct {
	gameMessage
	ReasonCode int `json:"reasonCode"`
}

type GameEndedMessage struct {
	gameMessage
	PlayerWinnerId string `json:"playerWinnerId"`
	GameId         string `json:"gameId"`
	GameTick       int    `json:"gameTick"`
	Map            Map    `json:"map"`
}

type SnakeDeadMessage struct {
	gameMessage
	PlayerId    string `json:"playerId"`
	X           int    `json:"x"`
	Y           int    `json:"y"`
	GameId      string `json:"gameId"`
	GameTick    int    `json:"gameTick"`
	DeathReason string `json:"deathReason"`
}

type GameStartingMessage struct {
	gameMessage
	NoOfPlayers int `json:"noofPlayers"`
	Width       int `json:"width"`
	Height      int `json:"height"`
}

type Map struct {
	Width  int      `json:"width"`
	Height int      `json:"height"`
	Map    [][]Tile `json:"tiles"`
}

type Tile struct {
	Content  string `json:"content"`
	PlayerId string `json:"PlayerId"`
}
