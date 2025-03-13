import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.Timer;
import java.util.Calendar;
import java.util.Date;

/**
 * @Author: phantom
 * @Description: 开启定时任务，指定时间，间隔给微信好友发送文本或图片
 * @Date: 2025/3/8 17:28
 */
public class TimerTask extends JFrame{
    // 设置定时任务区间,每隔一天发一次，将一天的毫秒数赋值给 SECTION
    private static TrayIcon trayIcon; // 添加托盘图标成员变量
    // 记录总任务数
    private static int totalTasks = 0;
    // 记录已执行的任务数
    private static int executedTasks = 0;

    // 在类中添加一个静态列表来存储任务详细信息
    private static List<String> taskDetails = new ArrayList<>();

    public static void main(String[] args)  throws Exception {

        // ▼在 main 方法开头添加▼
        String configPath = "C:/data"; // 默认存储路径
        // ▼在任务启动前添加路径校验▼
        File configFile = new File(configPath);
        if(!configFile.exists()) {
            JOptionPane.showMessageDialog(null,
                    "任务文件不存在，请重新选择",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object[] options = {"📁 导入已有任务文件", "✍ 手动输入任务"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "<html><b>请选择任务创建方式</b><br>推荐首次使用选择手动输入</html>",
                "启动模式选择",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );
        List<String> resource;
        if (choice == 0) {
            // ▼新增文件选择代码块▼
            JFileChooser fc = new JFileChooser();
            if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                configPath = fc.getSelectedFile().getPath();
                try {
                    resource = getResouce(configPath);
                } catch(FileNotFoundException e) {
                    JOptionPane.showMessageDialog(null,
                            "任务文件丢失，路径："+configPath,
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(null, "已取消选择", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            WriteToFile(configPath);   // STEP3 修改方法参数
            resource = getResouce(configPath);
        }

        // 隐藏Java默认的任务栏图标
        System.setProperty("java.awt.headless", "false");
        // 检查系统托盘支持
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "系统托盘不支持", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 初始化系统托盘
        createAndShowTray();

        // 使用 JOptionPane 显示欢迎信息
        JOptionPane.showMessageDialog(null, "欢迎使用phantom工具箱-微信定时发送工具", "提示", JOptionPane.INFORMATION_MESSAGE);
        JOptionPane.showMessageDialog(null, "程序有问题请关注公众号：幻影2021 点击联系我反馈问题", "提示", JOptionPane.INFORMATION_MESSAGE);
        // 使用 JOptionPane 显示任务执行时微信的状态要求
        JOptionPane.showMessageDialog(null, "任务执行时间，请保证微信在登录状态并为最小化", "提示", JOptionPane.INFORMATION_MESSAGE);
        JOptionPane.showMessageDialog(null, "任务设置完成后，可以双击系统托盘查看任务进度以及任务详细信息", "提示", JOptionPane.INFORMATION_MESSAGE);


        // 查询名为 "WeChat" 的进程数量
        int weChat = queryProcessCount("WeChat");
        // 如果微信进程数量小于等于 0，说明微信未登录，使用 JOptionPane 输出错误提示并返回
        if (weChat <= 0) {
            JOptionPane.showMessageDialog(null, "请登陆微信后再尝试运行", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        WriteToFile(configPath); // 添加路径参数

        // 使用 JOptionPane 显示输入成功信息
        JOptionPane.showMessageDialog(null, "输入成功，等待任务执行...", "提示", JOptionPane.INFORMATION_MESSAGE);

        // 获取当前年份
        int year = LocalDateTime.now().getYear();
        // 获取当前月份
        int month = LocalDateTime.now().getMonthValue();
        // 获取当前日期
        int day = LocalDateTime.now().getDayOfMonth();
        // 任务默认从今天开始


        // 创建日期格式化对象，用于将日期字符串解析为 Date 类型，包含秒
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        totalTasks = resource.size(); // 设置总任务数



        // 在 main 方法中，遍历任务资源列表时保存任务详细信息
        for (String todo : resource) {
            // 按空格分割任务字符串，获取任务时间等信息
            String[] item = todo.split(" ");
            // 拼接完整的日期时间字符串，直接使用文件中的日期信息
            String formatData = item[0] + " " + item[1] + ":" + item[2];
            // 将日期时间字符串解析为 Date 类型的对象
            Date firstData = simpleDateFormat.parse(formatData);
            // 创建一个列表，用于存储消息的接收者和内容信息
            List<Map<String, String>> sendData = new ArrayList<>();
            // 按分号分割任务字符串，获取多个消息项
            String[] sends = todo.split(";");
            int i = 0;
            // 遍历每个消息项
            for (String send : sends) {
                // 创建一个 Map 对象，用于存储单个消息的接收者和内容
                Map<String, String> map = new HashMap<>();
                // 按空格分割消息项，获取接收者和内容字符串列表
                List<String> strings = Arrays.asList(send.split(" "));
                // 如果是第一个消息项
                if (i == 0) {
                    // 将接收者信息存入 Map
                    map.put("receive", strings.get(2));
                    // 将内容信息存入 Map
                    map.put("content", strings.get(3));
                } else {
                    // 将接收者信息存入 Map
                    map.put("receive", strings.get(0));
                    // 将内容信息存入 Map
                    map.put("content", strings.get(1));
                }
                // 将单个消息的 Map 对象添加到 sendData 列表中
                sendData.add(map);
                i++;
            }

            // 保存任务详细信息
            StringBuilder taskDetail = new StringBuilder();
            taskDetail.append("执行时间: ").append(formatData).append("\n");
            for (Map<String, String> sendDatum : sendData) {
                taskDetail.append("接收者: ").append(sendDatum.get("receive")).append(", 内容: ").append(sendDatum.get("content")).append("\n");
            }
            taskDetails.add(taskDetail.toString());

            // 创建定时任务，传入任务开始时间和消息数据
            createTask(firstData, sendData);
        }
    }


    // 查询指定进程名称的进程数量
    private static int queryProcessCount(String processName) throws IOException {
        int count = 0;
        // 获取当前运行时对象
        Runtime runtime = Runtime.getRuntime();
        // 创建一个列表，用于存储任务列表信息
        List<String> tasklist = new ArrayList<>();
        // 执行 "tasklist" 命令，获取系统任务列表
        Process process = runtime.exec("tasklist");
        // 创建 BufferedReader 对象，用于读取命令执行结果
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        // 逐行读取命令执行结果
        while ((s = br.readLine()) != null) {
            // 如果行为空，跳过本次循环
            if ("".equals(s)) {
                continue;
            }
            // 将读取到的行添加到任务列表中
            tasklist.add(s);
        }
        // 遍历任务列表
        for (String taskName : tasklist) {
            // 如果任务名称包含指定的进程名称，进程数量加 1
            if (taskName.contains(processName)) {
                count++;
            }
        }
        // 返回进程数量
        return count;
    }

    // 创建定时任务
    private static void createTask(Date firstData, List<Map<String, String>> sendData) {
        if (firstData.getTime() - System.currentTimeMillis() < 0) {
            System.err.println("任务时间已过，无法执行该任务。");
            return;
        }
        new Timer().schedule(new java.util.TimerTask() {
            @Override
            public synchronized void run() {
                try {
                    openWeChat();
                    for (Map<String, String> sendDatum : sendData) {
                        sendMsg(sendDatum.get("receive"), sendDatum.get("content"));
                        Thread.sleep(500);
                    }
                    closeWeChat();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    synchronized (TimerTask.class) {
                        executedTasks++;
                        if (executedTasks == totalTasks) {
                            JOptionPane.showMessageDialog(null, "所有任务执行完毕，感谢使用phantom微信自动化工具，再见", "提示", JOptionPane.INFORMATION_MESSAGE);
                            System.exit(0);
                        }
                    }
                }
            }
        }, firstData.getTime() - System.currentTimeMillis());
    }

    // 设置系统剪贴板为文件内容（图片）
    public static void setSysClipboardFile(String imageUrl) throws IOException {
        // 获取系统剪贴板对象
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 如果图片路径包含反斜杠，将其替换为正斜杠
        if (imageUrl.contains("\\")) {
            imageUrl = imageUrl.replace("\\", "/");
        }
        // 去除图片路径中的特定前缀
        imageUrl = imageUrl.replace("img(", "");
        // 去除图片路径中的特定后缀
        imageUrl = imageUrl.substring(0, imageUrl.length() - 1);
        // 读取图片文件，获取 Image 对象
        Image image = ImageIO.read(new File(imageUrl));
        // 创建一个 Transferable 对象，用于将 Image 对象设置到剪贴板
        Transferable trans = new Transferable() {
            @Override
            public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException {
                // 如果支持指定的数据类型，返回 Image 对象
                if (isDataFlavorSupported(flavor)) {
                    return image;
                }
                // 否则抛出不支持数据类型的异常
                throw new UnsupportedFlavorException(flavor);
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                // 返回支持的数据类型数组，这里为 Image 类型
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                // 判断是否支持指定的数据类型
                return DataFlavor.imageFlavor.equals(flavor);
            }
        };
        // 将 Transferable 对象设置到系统剪贴板
        clip.setContents(trans, null);
    }

    // 设置系统剪贴板为文本内容
    public static void setSysClipboardText(String writeMe) {
        // 获取系统剪贴板对象
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 创建一个 StringSelection 对象，用于将文本设置到剪贴板
        Transferable tText = new StringSelection(writeMe);
        // 将 StringSelection 对象设置到系统剪贴板
        clip.setContents(tText, null);
    }

    // 打开微信应用程序
    private static void openWeChat() throws AWTException {
        // 获取 Robot 实例，用于模拟键盘操作
        Robot robot = RobotManager.getInstance();
        // 先使用 win+D 快捷键保证微信为最小化状态（代码被注释掉，未执行）
//        robot.keyPress(KeyEvent.VK_WINDOWS);
//        robot.keyPress(KeyEvent.VK_D);
//        robot.keyRelease(KeyEvent.VK_WINDOWS);
//        robot.keyRelease(KeyEvent.VK_D);
        // 再使用微信默认快捷键打开微信
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_W);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.keyRelease(KeyEvent.VK_W);
        // 线程休眠 100 毫秒
        robot.delay(100);
    }

    /**
     * 发送消息
     * @param receive 接收消息者
     * @param msg     消息内容
     */
    private static void sendMsg(String receive, String msg) throws Exception {
        // 获取 Robot 实例，用于模拟键盘操作
        Robot robot = RobotManager.getInstance();
        // 按下 Ctrl + F 快捷键（可能用于搜索好友）
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_F);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_F);
        // 输入接收者信息
        inputEnter(receive);
        // 输入消息内容
        inputEnter(msg);
    }

    // 关闭微信应用程序
    private static void closeWeChat() throws AWTException {
        // 获取 Robot 实例，用于模拟键盘操作
        Robot robot = RobotManager.getInstance();
        // 按下 Ctrl + Alt + W 快捷键关闭微信
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_W);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.keyRelease(KeyEvent.VK_W);
    }

    // 输入内容到微信聊天窗口
    private static void inputEnter(String msg) throws Exception {
        // 获取 Robot 实例，用于模拟键盘操作
        Robot robot = RobotManager.getInstance();
        // 如果消息内容包含图片路径标识
        if (msg.contains("img(")) {
            // 设置系统剪贴板为图片内容
            setSysClipboardFile(msg);
        } else {
            // 将消息中的换行符转义字符替换为实际换行符
            msg = msg.replace("\\n", "\n");
            // 设置系统剪贴板为文本内容
            setSysClipboardText(msg);
        }
        // 按下 Ctrl + V 快捷键粘贴内容
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_V);
        // 线程休眠 1000 毫秒
        robot.delay(1000);
        // 按下 Enter 键发送消息
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        // 线程休眠 500 毫秒
        robot.delay(500);
    }

    // Robot 实例管理类
    public static class RobotManager {
        // 静态成员变量，存储 Robot 实例
        private static Robot robot;

        // 获取 Robot 实例的方法
        public static Robot getInstance() throws AWTException {
            // 如果 Robot 实例为空，创建一个新的实例
            if (robot == null) {
                robot = new Robot();
            }
            // 返回 Robot 实例
            return robot;
        }
    }

    // 从指定文件中获取任务资源信息
    // ▼修改方法签名+路径参数化▼
    public static List<String> getResouce(String filePath) throws Exception {
        // 创建一个 StringBuilder 对象，用于拼接文件内容
        StringBuilder result = new StringBuilder();
        // 创建 FileReader 对象，用于读取指定文件（文件路径需根据实际情况修改）
        FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8);
        // 创建 BufferedReader 对象，用于读取文件内容
        BufferedReader br = new BufferedReader(reader);
        String temp;
        // 逐行读取文件内容
        while ((temp = br.readLine()) != null) {
            // 使用 readLine 方法，一次读一行，并将内容追加到 StringBuilder 中
            result.append(temp);
        }
        // 关闭 BufferedReader
        br.close();
        // 关闭 FileReader
        reader.close();
        // 将拼接后的字符串按星号分割为列表，并返回
        return Arrays.asList(result.toString().split("\\*"));
    }

    // ▼修改方法参数和实现▼
    public static void WriteToFile(String savePath) { // 新增路径参数
        try {
            // 使用 JOptionPane 提示用户输入任务数量
            String numInput = JOptionPane.showInputDialog(null, "请输入任务数量：", "输入任务数量", JOptionPane.PLAIN_MESSAGE);
            int num = Integer.parseInt(numInput);

            try (FileWriter writer = new FileWriter(savePath)) {
                for (int i = 0; i < num; i++) {
                    // 创建自定义输入面板
                    JPanel panel = createDateTimePanel();
                    int result = JOptionPane.showConfirmDialog(null, panel, "输入第 " + (i + 1) + " 个任务的时间", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        // 获取用户选择的年月日时分秒
                        JSpinner yearSpinner = (JSpinner) panel.getComponent(1);
                        JSpinner monthSpinner = (JSpinner) panel.getComponent(3);
                        JSpinner daySpinner = (JSpinner) panel.getComponent(5);
                        JSpinner hourSpinner = (JSpinner) panel.getComponent(7);
                        JSpinner minuteSpinner = (JSpinner) panel.getComponent(9);
                        JSpinner secondSpinner = (JSpinner) panel.getComponent(11);

                        int year = (int) yearSpinner.getValue();
                        int month = (int) monthSpinner.getValue();
                        int day = (int) daySpinner.getValue();
                        int hour = (int) hourSpinner.getValue();
                        int minute = (int) minuteSpinner.getValue();
                        int second = (int) secondSpinner.getValue();

                        writer.write(String.format("%04d-%02d-%02d %02d:%02d:%02d ", year, month, day, hour, minute, second));

                        // 使用 JOptionPane 提示用户输入任务的接收者
                        String people = JOptionPane.showInputDialog(null, "请输入第 " + (i + 1) + " 个任务的接收者（务必完全一样，不然有可能发错）", "输入任务信息", JOptionPane.PLAIN_MESSAGE);
                        writer.write(people);
                        writer.write(" ");

                        // 使用 JOptionPane 提示用户输入任务的消息内容
                        String con = JOptionPane.showInputDialog(null, "请输入第 " + (i + 1) + " 个任务的消息内容", "输入任务信息", JOptionPane.PLAIN_MESSAGE);
                        writer.write(con);

                        if (i != (num - 1)) {
                            writer.write("*\n");
                        }
                        JOptionPane.showMessageDialog(null, "第 " + (i + 1) + " 个任务写入成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                // ▽在最后写入成功后提示文件位置▽
                JOptionPane.showMessageDialog(null,
                        "任务已保存至：" + new File(savePath).getAbsolutePath(),
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "输入的任务数量或时间格式不正确，请重新输入。", "错误", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "写入文件时出错: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 创建系统托盘图标
    private static void createAndShowTray() {
        // 获取系统托盘实例
        final SystemTray tray = SystemTray.getSystemTray();

        // 加载托盘图标图片（需要准备一个ico文件）
        Image image = Toolkit.getDefaultToolkit().getImage(TimerTask.class.getResource("Desktop.jpg"));

        // 创建弹出菜单
        PopupMenu popup = new PopupMenu();
        // 创建菜单项
        MenuItem exitItem = new MenuItem("exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });

        popup.add(exitItem);

        // 创建托盘图标
        trayIcon = new TrayIcon(image, "微信定时发送工具", popup);
        trayIcon.setImageAutoSize(true);

        // 双击事件处理
        // 双击事件处理
        trayIcon.addActionListener(e -> {
            StringBuilder message = new StringBuilder();
            message.append("当前任务进度：").append(executedTasks).append("/").append(totalTasks).append("\n\n");
            for (int i = 0; i < taskDetails.size(); i++) {
                message.append("任务 ").append(i + 1).append(":\n");
                message.append(taskDetails.get(i)).append("\n");
            }
            JOptionPane.showMessageDialog(null, message.toString(), "任务状态", JOptionPane.INFORMATION_MESSAGE);
        });

        try {
            tray.add(trayIcon);
            trayIcon.displayMessage("微信定时发送工具", "程序已最小化到系统托盘", TrayIcon.MessageType.INFO);
        } catch (AWTException e) {
            System.err.println("无法添加系统托盘图标");
        }
    }

    // 创建包含年月日时分秒选择器的面板
    private static JPanel createDateTimePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 2));

        // 获取当前日期和时间
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentSecond = calendar.get(Calendar.SECOND);

        // 年选择器
        JLabel yearLabel = new JLabel("年:");
        SpinnerModel yearModel = new SpinnerNumberModel(currentYear, currentYear - 100, currentYear + 100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        panel.add(yearLabel);
        panel.add(yearSpinner);

        // 月选择器
        JLabel monthLabel = new JLabel("月:");
        SpinnerModel monthModel = new SpinnerNumberModel(currentMonth, 1, 12, 1);
        JSpinner monthSpinner = new JSpinner(monthModel);
        panel.add(monthLabel);
        panel.add(monthSpinner);

        // 日选择器
        JLabel dayLabel = new JLabel("日:");
        SpinnerModel dayModel = new SpinnerNumberModel(currentDay, 1, 31, 1);
        JSpinner daySpinner = new JSpinner(dayModel);
        panel.add(dayLabel);
        panel.add(daySpinner);

        // 时选择器
        JLabel hourLabel = new JLabel("时:");
        SpinnerModel hourModel = new SpinnerNumberModel(currentHour, 0, 23, 1);
        JSpinner hourSpinner = new JSpinner(hourModel);
        panel.add(hourLabel);
        panel.add(hourSpinner);

        // 分选择器
        JLabel minuteLabel = new JLabel("分:");
        SpinnerModel minuteModel = new SpinnerNumberModel(currentMinute, 0, 59, 1);
        JSpinner minuteSpinner = new JSpinner(minuteModel);
        panel.add(minuteLabel);
        panel.add(minuteSpinner);

        // 秒选择器
        JLabel secondLabel = new JLabel("秒:");
        SpinnerModel secondModel = new SpinnerNumberModel(currentSecond, 0, 59, 1);
        JSpinner secondSpinner = new JSpinner(secondModel);
        panel.add(secondLabel);
        panel.add(secondSpinner);

        return panel;
    }

}