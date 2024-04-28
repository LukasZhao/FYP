package players.reinforcement;

public class WinRateCalculator {
    private static int totalWins = 0;
    private static int totalLosses = 0;
    private static int totalDraws = 0;
    private static int totalGames = 0;

    // 增加胜利的次数
    public static void addWin() {
        totalWins++;
        totalGames++;
    }

    // 增加失败的次数
    public static void addLoss() {
        totalLosses++;
        totalGames++;
    }

    // 增加平局的次数
    public static void addDraw() {
        totalDraws++;
        totalGames++;
    }

    // 计算并获取胜率
    public static double calculateWinRate() {
        if (totalGames == 0)
            return 0.0; // 避免除以零
        return (double) totalWins / totalGames * 100.0; // 转换为百分比
    }

    // 输出胜率及胜利、失败和平局的次数
    public static void printStatistics() {
        System.out.println("Total Games Played: " + totalGames);
        System.out.println("Wins: " + totalWins);
        System.out.println("Losses: " + totalLosses);
        System.out.println("Draws: " + totalDraws);
        System.out.println("Win Rate: " + calculateWinRate() + "%");
    }
}
