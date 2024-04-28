package players.reinforcement;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Arrays;

public class QTableVisualizer extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public QTableVisualizer() {
        // 设置 JFrame 基本属性
        setTitle("Q-Table Visualization");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 初始化 JTable 和 DefaultTableModel
        model = new DefaultTableModel();
        model.addColumn("State");
        model.addColumn("Action");
        model.addColumn("Q-value");
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    // 这个方法接受新的 Q 表数据并更新表格
    public void updateQTable(Map<String, double[]> qTable) {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0); // 清空表格以准备添加新数据
            qTable.forEach((stateAction, qValues) -> {
                // 分解状态和动作
                String[] parts = stateAction.split(":");
                String state = parts[0] + ":" + parts[1];
                String action = parts[2];
                double qValue = qValues[1]; // 假设索引 1 总是对应最佳动作

                // 向表格模型添加行
                model.addRow(new Object[] { state, action, qValue });
            });
        });
    }
}
