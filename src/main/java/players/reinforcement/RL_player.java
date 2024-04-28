
package players.reinforcement;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.interfaces.IStateHeuristic;
import games.GameType;
import games.blackjack.BlackjackForwardModel;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import net.jpountz.util.Utils;
import players.PlayerType;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;
import core.CoreConstants;
import utilities.Pair;
import static games.GameType.Blackjack;

import java.awt.SystemColor;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.AbstractGameState;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.AbstractGameState;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.avro.hadoop.file.SortedKeyValueFile.Reader;

public class RL_player extends AbstractPlayer {
    private double alpha; // 学习率
    private double gamma; // 折扣因子
    private double epsilon; // 探索率
    private double final_epsilon; // 最终 epsilon
    private double epsilon_decay; // epsilon 衰减速率
    public final Map<String, Double> qTable; // 状态-行动对的Q值
    private final Random random;
    private static int totalIterations = 20000;

    public RL_player(double alpha, double gamma, double epsilon, long seed) {
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.qTable = new HashMap<>();
        this.random = new Random(seed);
        this.final_epsilon = 0.1;
        this.epsilon_decay = epsilon / (totalIterations / 0.68); // 假设衰减发生在总迭代次数的一半时间里
    }

    public void setPlayerID(int id) {
        this.playerID = id;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        //epsilon = Math.max(final_epsilon, epsilon - epsilon_decay);
        //System.out.println("epsilon is" + epsilon);
        if (random.nextDouble() < epsilon) {
            // 探索: 随机选择行动
            return possibleActions.get(random.nextInt(possibleActions.size()));
        } else {
            // 利用: 根据Q值选择最好的行动
            return getBestAction((BlackjackGameState) gameState, possibleActions);
        }
    }

    private AbstractAction getBestAction(BlackjackGameState gameState, List<AbstractAction> possibleActions) {
        String state = encodeState(gameState);
        AbstractAction bestAction = null;
        double maxQ = Double.NEGATIVE_INFINITY;

        for (AbstractAction action : possibleActions) {
            String actionKey = state + action.toString();
            double qValue = qTable.getOrDefault(actionKey, 0.0);
            if (qValue > maxQ) {
                maxQ = qValue;
                bestAction = action;
            }
        }
        return bestAction != null ? bestAction : possibleActions.get(random.nextInt(possibleActions.size()));
    }

    public void updateQTable(String state, AbstractAction action, double reward, String nextState, boolean done) {
        String actionKey = state + ":" + action.toString();
        double q = qTable.getOrDefault(actionKey, 0.0); // 获取当前 Q 值
        double maxQNext = done ? 0 : getMaxQ(nextState); // 计算下一个状态的最大 Q 值

        // 计算 Temporal Difference (TD) error
        double tdError = reward + gamma * maxQNext - q;

        // 更新 Q 值，加入学习率 alpha
        double newQ = q + alpha * tdError; // 使用TD error 更新 Q 值
        qTable.put(actionKey, newQ); // 存储新的 Q 值
    }

  
    private double getMaxQ(String nextState) {
        return qTable.entrySet().stream()
                .filter(e -> e.getKey().startsWith(nextState))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getValue)
                .orElse(0.0);
    }

    public String encodeState(BlackjackGameState gameState) {
        int playerPoints = gameState.calculatePoints(getPlayerID());
        // 获取庄家的可见牌
        FrenchCard dealerVisibleCard = (FrenchCard) gameState.getDrawDeck().peek();
        // 使用正确的number属性代替不存在的rank属性
        int dealerVisibleValue = dealerVisibleCard != null ? dealerVisibleCard.number : -1;

        return playerPoints + ":" + dealerVisibleValue;
    }

    public double calculateReward(BlackjackGameState gameState, int playerId) {
        CoreConstants.GameResult playerResult = gameState.getPlayerResults()[playerId];
        int playerPoints = gameState.calculatePoints(playerId); // Assume getPlayerPoints() method exists to fetch
                                                                  // points
        double reward = 0.0;

        switch (playerResult) {
            case WIN_GAME:
                if (playerPoints == 21) {
                    reward = 2.0; // Additional reward for hitting exactly 21 points
                } else {
                    reward = 1.0;
                }
                break;
            case LOSE_GAME:
                reward = -1.0;
                break;
            case DRAW_GAME:
                reward = 0; // Or any other appropriate value as per game design
                break;
            default:
                reward = 0.0; // Game is still ongoing
                break;
        }
        // Uncomment below line to debug and track reward values
        // System.out.println("Reward calculated: " + reward);
        return reward;
    }

    @Override
    public AbstractPlayer copy() {
        RL_player clone = new RL_player(alpha, gamma, epsilon, random.nextLong());
        clone.qTable.putAll(this.qTable);
        return clone;
    }
    // 创建图表
    // RewardChart chart = new RewardChart("RL Training Reward Progress");
    // 更新奖励图表
    // chart.updateChart(i, reward);
    public static void main(String[] args) {
        double win = 0.0;
        double lose = 0.0;
        double draw = 0.0;
        RL_player rlPlayer = new RL_player(0.8, 0.95, 0.7, System.currentTimeMillis());
        // 初始化前向模型
        BlackjackForwardModel model = new BlackjackForwardModel();
        RewardChart chart = new RewardChart("RL Training Reward Progress", totalIterations);

        for (int i = 0; i < totalIterations; i++) {
            long seed = System.currentTimeMillis(); // 更新种子以保证随机性
            BlackjackParameters params = new BlackjackParameters(seed);
  //那为什么他也会有(25, 5, 0):
            // 初始化游戏状态
            BlackjackGameState gameState = new BlackjackGameState(params, 2); // 假设有2个玩家
            model.setup(gameState); // 游戏开始的初始化
            while (!gameState.isGameOver()) {
                List<AbstractAction> possibleActions = model.computeAvailableActions(gameState);
                AbstractAction chosenAction = rlPlayer.getAction(gameState, possibleActions);
                model.performAction(gameState, chosenAction);
                if (gameState.isGameOver()) {
                    double reward = rlPlayer.calculateReward(gameState, rlPlayer.getPlayerID());
                    if(reward == -1.0){lose ++;}
                    else if(reward == 1.0){win ++;}
                    else{draw ++;}
                    String nextState = rlPlayer.encodeState(gameState);
                    rlPlayer.updateQTable(rlPlayer.encodeState(gameState), chosenAction, reward, nextState,
                            gameState.isGameOver());
                    chart.updateChart(i, reward);
                    break;
                }
            }
        }
        System.out.println("Training completed.");
        double win_rate = (win/totalIterations)*100;
        double lose_rate = (lose/totalIterations)*100;
        System.out.println(draw);
        System.out.println(win_rate + "%");
        System.out.println(lose_rate + "%");
        chart.displayChart();
    }

}