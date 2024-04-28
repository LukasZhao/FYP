package players.reinforcement;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModelTester {
    private RL_player rlPlayer;
    private BlackjackForwardModel model;
    private int totalIterations;

    public ModelTester(RL_player player, int iterations) {
        this.rlPlayer = player;
        this.model = new BlackjackForwardModel();
        this.totalIterations = iterations;
    }

    public void testModel() {
        double win = 0.0;
        double lose = 0.0;
        double draw = 0.0;
        for (int i = 0; i < totalIterations; i++) {
            long seed = System.currentTimeMillis(); // 更新种子以保证随机性
            BlackjackParameters params = new BlackjackParameters(seed);

            // 初始化游戏状态
            BlackjackGameState gameState = new BlackjackGameState(params, 2); // 假设有2个玩家
            model.setup(gameState); // 游戏开始的初始化
            ArrayList<AbstractPlayer> players = new ArrayList<>();
            players.add(rlPlayer);
            players.add(rlPlayer);
            while (!gameState.isGameOver()) {
                List<AbstractAction> possibleActions = model.computeAvailableActions(gameState);
                AbstractAction chosenAction = rlPlayer.getAction(gameState, possibleActions);
                model.performAction(gameState, chosenAction);

            if (gameState.isGameOver()) {
                double reward = rlPlayer.calculateReward(gameState, rlPlayer.getPlayerID());
                if (reward == -1.0) {
                    lose++;
                } else if (reward == 1.0) {
                    win++;
                } else {
                    draw++;
                }
            }
        }
            }
        System.out.println("Testing completed.");
        double winRate = (win / totalIterations) * 100;
        double loseRate = (lose / totalIterations) * 100;
        double drawRate = (draw / totalIterations) * 100;

        System.out.println("Win Rate: " + winRate + "%");
        System.out.println("Lose Rate: " + loseRate + "%");
        System.out.println("Draw Rate: " + drawRate + "%");
    }
    public static void main(String[] args) {
        RL_player RL = new RL_player(0, 0, 1, 0);
        RL_player playerCopy = (RL_player) RL.copy();
        ModelTester tester = new ModelTester(playerCopy, 100000);

        tester.testModel();
    }
}
