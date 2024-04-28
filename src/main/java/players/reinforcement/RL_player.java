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
import players.reinforcement.WinRateCalculator;

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
    private Map<String, double[]> qTable;
    // 状态-行动对的Q值
    private final Random random;
    private static int totalIterations = 20000;

    public RL_player(double alpha, double gamma, double epsilon, long seed) {
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.qTable = new HashMap<>();
        this.random = new Random(seed);
        this.final_epsilon = 0.1;
        this.epsilon_decay = epsilon / (totalIterations / 0.58); // 假设衰减发生在总迭代次数的一半时间里
    }

    public void setPlayerID(int id) {
        this.playerID = id;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        epsilon = Math.max(final_epsilon, epsilon - epsilon_decay);
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
            double[] qValues = qTable.getOrDefault(actionKey, new double[] { 0.0, 0.0 });

            // 假设数组的第一个元素是“停牌”，第二个元素是“要牌”
            int actionIndex = action instanceof Stand ? 0 : 1;
            double qValue = qValues[actionIndex];

            if (qValue > maxQ) {
                maxQ = qValue;
                bestAction = action;
            }
        }
        return bestAction != null ? bestAction : possibleActions.get(random.nextInt(possibleActions.size()));
    }

    public void updateQTable(String state, AbstractAction action, double reward, String nextState, boolean done) {
        String actionKey = state + ":" + action.toString();
        // getOrDefault 应该返回一个 double[] 类型的数组，而不是 Double
        double[] qValues = qTable.getOrDefault(actionKey, new double[] { 0.0, 0.0 });

        // 确定动作索引
        int actionIndex = action instanceof Stand ? 0 : 1;

        double q = qValues[actionIndex]; // 获取当前动作的 Q 值
        double maxQNext = done ? 0 : getMaxQ(nextState); // 计算下一个状态的最大 Q 值

        // 计算 Temporal Difference (TD) error
        double tdError = reward + gamma * maxQNext - q;

        // 更新 Q 值，加入学习率 alpha
        qValues[actionIndex] = q + alpha * tdError; // 更新对应动作的 Q 值

        // 存储新的 Q 值数组回 qTable
        qTable.put(actionKey, qValues);
    }

    private double getMaxQ(String nextState) {
        // 从 qTable 中检索所有与 nextState 开头的条目，并找到最大的 Q 值
        double maxQ = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, double[]> entry : qTable.entrySet()) {
            if (entry.getKey().startsWith(nextState)) {
                double[] qValues = entry.getValue();
                for (double q : qValues) {
                    if (q > maxQ) {
                        maxQ = q;
                    }
                }
            }
        }
        return maxQ;
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
        RL_player rlPlayer = new RL_player(0.2, 0.95, 1, System.currentTimeMillis());
        // 初始化前向模型
        BlackjackForwardModel model = new BlackjackForwardModel();
        RewardChart chart = new RewardChart("RL Training Reward Progress", totalIterations);
        QTableVisualizer visualizer = new QTableVisualizer();

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
                    // 更新 Q-表的视图
                    //visualizer.updateQTable(rlPlayer.qTable);
                    break;
                }
            }

            // 输出当前迭代次数以验证
            // System.out.println("Iteration: " + i+1);
            //visualizer.updateQTable(rlPlayer.qTable, i + 1);
        }
        System.out.println("Training completed.");
        double win_rate = (win/totalIterations)*100;
        double lose_rate = (lose/totalIterations)*100;
        System.out.println(draw);
        System.out.println(win_rate + "%");
        System.out.println(lose_rate + "%");
        chart.displayChart();
        
        // rlPlayer.qTable.forEach((key, value) -> System.out.println(key + ": " +
        // value));

    }
}
