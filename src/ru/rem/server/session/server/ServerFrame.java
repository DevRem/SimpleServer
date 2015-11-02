package ru.rem.server.session.server;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.SocketAddress;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class ServerFrame extends JFrame{
  
    private JScrollPane jScrollPane;
    private JTable jTable;
    private UserList userList;
    private JButton jPath;
    private JTextField jTextFieldPath;
    
    public ServerFrame() {
        super("Сервер");
        initComponents();
    }
    
    private void initComponents() {
        
        jScrollPane = new JScrollPane();
        jTable = new JTable();
        jTextFieldPath = new JTextField();
        jPath = new JButton();

        setForeground(Color.black);
        setBackground(Color.lightGray);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        
        jTable.setModel(userList = new UserList());
        
        jScrollPane.setViewportView(jTable);
        jTextFieldPath.setEditable(false);
        jTextFieldPath.setText("Укажите каталог...");
        jPath.setText("Путь");
        
        actionsPerformed();
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextFieldPath, GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                        .addComponent(jPath)))
                .addContainerGap())
        );
        
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextFieldPath, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPath, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane, GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }
    
    public void actionsPerformed(){
        
        jPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                JFileChooser dialog = updateUI(new JFileChooser());
                dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                dialog.setDialogTitle("Выберите каталог...");
                dialog.setMultiSelectionEnabled(false);
                int ret = dialog.showDialog(null, "Выбрать");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String catalog = dialog.getSelectedFile().getPath() + File.separatorChar;
                    jTextFieldPath.setText(catalog);
                    SimpleServer.PATH = catalog;
                    start();
                }
            }
        });
    }
    
    private void start() {
        if (!SimpleServer.PATH.isEmpty()) {
            jPath.setEnabled(false);
            Thread t = new Thread(new SimpleServer(this));
            t.setDaemon(true);
            t.start();
        }
    }

    public synchronized UserList getUserList(){
        return userList;
    }
    
    public void addSession(final SocketAddress clientAdress, final SocketChannelHandler sc){
        SwingUtilities.invokeLater(() -> {
            getUserList().addSession(clientAdress, sc);
            jTable.updateUI();
        });
    }
    
    public static void main(String[] args){
        
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception  ex) {
            
        }
        
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ServerFrame().setVisible(true);
            }
        });
        
    }
    
    public JFileChooser updateUI(JFileChooser choose) {
       
        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.lookInLabelText", "Смотреть в");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файла");

        UIManager.put("FileChooser.saveButtonText", "Сохранить");
        UIManager.put("FileChooser.saveButtonToolTipText", "Сохранить");
        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.openButtonToolTipText", "Открыть");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.cancelButtonToolTipText", "Отмена");

        UIManager.put("FileChooser.lookInLabelText", "Папка");
        UIManager.put("FileChooser.saveInLabelText", "Папка");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файлов");

        UIManager.put("FileChooser.upFolderToolTipText", "На один уровень вверх");
        UIManager.put("FileChooser.newFolderToolTipText", "Создание новой папки");
        UIManager.put("FileChooser.listViewButtonToolTipText", "Список");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Таблица");
        UIManager.put("FileChooser.fileNameHeaderText", "Имя");
        UIManager.put("FileChooser.fileSizeHeaderText", "Размер");
        UIManager.put("FileChooser.fileTypeHeaderText", "Тип");
        UIManager.put("FileChooser.fileDateHeaderText", "Изменен");
        UIManager.put("FileChooser.fileAttrHeaderText", "Атрибуты");
        UIManager.put("FileChooser.acceptAllFileFilterText", "Все файлы");
        
        choose.updateUI();
        return choose;
    }
    
}
