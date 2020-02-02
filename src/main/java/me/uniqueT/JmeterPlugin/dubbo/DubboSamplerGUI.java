package me.uniqueT.JmeterPlugin.dubbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledChoice;
import org.apache.jorphan.gui.JLabeledTextField;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.uniqueT.JmeterPlugin.dubbo.reflect.TempletHelper;
import me.uniqueT.JmeterPlugin.dubbo.util.JsonFormatTool;

public class DubboSamplerGUI extends AbstractSamplerGui implements ActionListener {
	
	private final JLabeledTextField ipText = new JLabeledTextField("ip :");
	private final JLabeledTextField portText = new JLabeledTextField("port :");
	private final JLabeledTextField infnameText = new JLabeledTextField("interface name :");
	private final JLabeledTextField methodText = new JLabeledTextField("invoke method :");
	private final JSyntaxTextArea textArea = JSyntaxTextArea.getInstance(22, 50);
	private final JButton beautifyBtn = new JButton("请求格式化");
	private final JButton templeteBtn = new JButton("生成模板");
	private final String[] impChoice = {"Telnet Client", "Generic Service"};
	private final JLabeledChoice impClass = new JLabeledChoice("implement :",impChoice);
	private final JLabeledTextField timeoutText = new JLabeledTextField("timeout(ms) :");
	private JTabbedPane jTabbedpane;
	private JTable argTable;
	private DefaultTableModel tableModel;
	private final String SAMPLER_NAME = "Dubbo Sampler";

	/**
	 * 
	 */
	private static final long serialVersionUID = 6604593205125989898L;
	
	public DubboSamplerGUI(){
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH);
		
		JPanel mainPanel = new JPanel();
		add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.setLayout(new BorderLayout());
		
		JPanel header = new JPanel();
		header.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "Interface Infomation"));
		
		mainPanel.add(header, BorderLayout.NORTH);
		GridBagLayout gbl_header = new GridBagLayout();
		gbl_header.columnWeights = new double[]{1, 1, 1, 1, 1, 1};
		gbl_header.rowWeights = new double[]{1, 1, 1};
		header.setLayout(gbl_header);
		
		//tianchong 
		GridBagConstraints gbc_textField_0 = new GridBagConstraints();
		gbc_textField_0.fill = GridBagConstraints.BOTH;
		gbc_textField_0.insets = new Insets(0, 0, 5, 5);
		gbc_textField_0.gridx = 0;
		gbc_textField_0.gridy = 0;
		header.add(new JPanel(), gbc_textField_0);
		
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.fill = GridBagConstraints.BOTH;
		gbc_textField_1.insets = new Insets(0, 0, 5, 5);
		gbc_textField_1.gridwidth = 3;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 0;
		header.add(ipText, gbc_textField_1);
		
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.fill = GridBagConstraints.BOTH;
		gbc_textField_2.insets = new Insets(0, 50, 5, 5);
		gbc_textField_2.gridx = 4;
		gbc_textField_2.gridy = 0;
		header.add(portText, gbc_textField_2);
		
		//tianchong 
		GridBagConstraints gbc_textField_5 = new GridBagConstraints();
		gbc_textField_5.fill = GridBagConstraints.BOTH;
		gbc_textField_5.insets = new Insets(0, 0, 5, 5);
		gbc_textField_5.gridx = 5;
		gbc_textField_5.gridy = 0;
		header.add(new JPanel(), gbc_textField_5);
		
		GridBagConstraints gbc_infnameField = new GridBagConstraints();
		gbc_infnameField.fill = GridBagConstraints.BOTH;
		gbc_infnameField.insets = new Insets(0, 0, 5, 5);
		gbc_infnameField.gridwidth = 4;
		gbc_infnameField.gridx = 1;
		gbc_infnameField.gridy = 1;
		header.add(infnameText, gbc_infnameField);
		
		GridBagConstraints gbc_methodText = new GridBagConstraints();
		gbc_methodText.fill = GridBagConstraints.BOTH;
		gbc_methodText.insets = new Insets(0, 0, 5, 5);
		gbc_methodText.gridwidth = 2;
		gbc_methodText.gridx = 1;
		gbc_methodText.gridy = 2;
		header.add(methodText, gbc_methodText);
		
		impClass.setPreferredSize(new Dimension(200, 32));
		GridBagConstraints gbc_acField = new GridBagConstraints();
		gbc_acField.anchor = GridBagConstraints.WEST;
		gbc_acField.insets = new Insets(-5, 45, 5, 5);
		gbc_acField.gridx = 3;
		gbc_acField.gridy = 2;
		header.add(impClass, gbc_acField);
		
		GridBagConstraints gbc_timeoutText = new GridBagConstraints();
		gbc_timeoutText.fill = GridBagConstraints.BOTH;
		gbc_timeoutText.insets = new Insets(0, 0, 5, 5);
		gbc_timeoutText.gridx = 4;
		gbc_timeoutText.gridy = 2;
		header.add(timeoutText, gbc_timeoutText);
		
		beautifyBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String inputString;
				if(textArea.getSelectedText()==null){
					inputString = textArea.getText();
				}
				else{
					inputString = textArea.getSelectedText();
				}
				if(!inputString.contains(":"))
					return;
				Gson gson = new Gson();
				JsonParser jp = new JsonParser();
				JsonFormatTool jft = new JsonFormatTool();
				String beautyJSON;
				try{
					JsonElement je = jp.parse(inputString);
					beautyJSON = gson.toJson(je);
					beautyJSON = jft.formatJson(beautyJSON);
				}catch(JsonSyntaxException jse){
					return;
				}
				if(textArea.getSelectedText()==null){
					textArea.setText(beautyJSON);
				}else{
					textArea.replaceSelection(beautyJSON);
				}
			}
		});
		
		templeteBtn.addActionListener(this);
		
		jTabbedpane = new JTabbedPane();
		jTabbedpane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "Arguments"));
		JPanel textInputPanel = new VerticalPanel(1, 0.1f);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		JTextScrollPane scrollInput = JTextScrollPane.getInstance(textArea);
		JPanel bp = new JPanel();
		bp.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 0));
		bp.add(beautifyBtn);
		bp.add(templeteBtn);
		textInputPanel.add(bp);
		textInputPanel.add(scrollInput);
		jTabbedpane.addTab("text input", textInputPanel);
		
		JPanel formatInputPanel = new JPanel();
		formatInputPanel.setLayout(new BorderLayout());
		argTable = new JTable();
		initTableData();
		JPanel tableBtns = new VerticalPanel();
		JButton addRowBtn = new JButton("添加");
		addRowBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				tableModel.addRow(new String[2]);
			}
		});
		tableBtns.add(addRowBtn);
		JButton delRowBtn = new JButton("删除");
		delRowBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int[] selected = argTable.getSelectedRows();
				for(int i : selected){
					tableModel.removeRow(i);
				}
			}
		});
		tableBtns.add(delRowBtn);
		JButton upRowBtn = new JButton("上移");
		upRowBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int[] selected = argTable.getSelectedRows();
				tableModel.moveRow(selected[0], selected[selected.length-1], selected[0]-1);
				argTable.setRowSelectionInterval(selected[0]-1, selected[selected.length-1]-1);
			}
		});
		tableBtns.add(upRowBtn);
		JButton downRowBtn = new JButton("下移");
		downRowBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int[] selected = argTable.getSelectedRows();
				tableModel.moveRow(selected[0], selected[selected.length-1], selected[0]+1);
				argTable.setRowSelectionInterval(selected[0]+1, selected[selected.length-1]+1);
			}
		});
		tableBtns.add(downRowBtn);
		formatInputPanel.add(new JScrollPane(argTable), BorderLayout.CENTER);
		formatInputPanel.add(tableBtns, BorderLayout.EAST);
		jTabbedpane.addTab("format input", formatInputPanel);
		jTabbedpane.setEnabledAt(1, false);
		impClass.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if("Generic Service".equals(impClass.getText())){
					jTabbedpane.setEnabledAt(1, true);
				}else{
					jTabbedpane.setEnabledAt(1, false);
					jTabbedpane.setSelectedIndex(0);
				}
			}
		});
		
		mainPanel.add(jTabbedpane, BorderLayout.CENTER);
		
		/*JPanel textP = new VerticalPanel(1, 0.1f);
		textP.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "Arguments"));
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		JTextScrollPane tapanel = JTextScrollPane.getInstance(textArea);
		JPanel bp = new JPanel();
		bp.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
		bp.add(beautifyBtn);
		textP.add(bp);
		textP.add(tapanel);
		mainPanel.add(textP, BorderLayout.CENTER);*/
		
		setName(SAMPLER_NAME);
	}

	@Override
	public TestElement createTestElement() {
		DubboSampler ds = new DubboSampler();
		modifyTestElement(ds);
		return ds;
	}

	@Override
	public String getLabelResource() {
		//not been called yet
		return this.getClass().getSimpleName();
	}

	//data:from gui to sampler
	@Override
	public void modifyTestElement(TestElement te) {
		super.configureTestElement(te);
		DubboSampler ds = (DubboSampler)te;
		setupSamplerProperties(ds);
	}
	
	//data:from sampler to gui
	@Override
	public void configure(TestElement te) {
		super.configure(te);
		DubboSampler ds = (DubboSampler)te;
		ipText.setText(ds.getIp());
		portText.setText(String.valueOf(ds.getPort()));
		infnameText.setText(ds.getInfName());
		textArea.setText(ds.getInvokeCMD());
		impClass.setText(ds.getImpClass());
		methodText.setText(ds.getMethod());
		setTableData(ds.getArgTable());
		jTabbedpane.setSelectedIndex(ds.getArgType());
		timeoutText.setText(ds.getTimeout());
	}
	
	private void setupSamplerProperties(DubboSampler sampler){
		sampler.setIp(ipText.getText().trim());
		sampler.setPort(portText.getText().trim());
		sampler.setInfName(infnameText.getText().trim());
		sampler.setInvokeCMD(textArea.getText());
		sampler.setImpClass(impClass.getText());
		sampler.setMethod(methodText.getText().trim());
		sampler.setArgTable(getTableData());
		sampler.setArgType(jTabbedpane.getSelectedIndex());
		sampler.setTimeout(timeoutText.getText().trim());
	}
	
	private String[][] getTableData() {
		/*LinkedList<LinkedList<String>> list = new LinkedList<LinkedList<String>>();
		int rowCount = tableModel.getRowCount();
		for(int x=0;x<rowCount;x++){
			LinkedList<String> l = new LinkedList<String>();
			l.add((String)tableModel.getValueAt(x, 0));
			l.add((String)tableModel.getValueAt(x, 1));
			list.add(l);
		}*/
		int rowCount = tableModel.getRowCount();
		int columnCount = tableModel.getColumnCount();
		String[][] result = new String[rowCount][columnCount];
		for(int x=0;x<rowCount;x++){
			for(int y=0;y<columnCount;y++){
				result[x][y] = (String)tableModel.getValueAt(x, y);
			}
		}
		return result;
	}
	
	private void setTableData(String[][] tableData){
		String[] columnNames = {"参数类型","参数值"};
		/*String[][] dataArray = new String[paramList.size()][2];
		int i = 0;
		for(LinkedList<String> l : paramList){
			dataArray[i][0] = l.get(0);
			dataArray[i][1] = l.get(1);
			i++;
		}*/
		tableModel = new DefaultTableModel(tableData, columnNames);
		argTable.setModel(tableModel);
	}
	
	private void initTableData(){
		tableModel = new DefaultTableModel();
		argTable.setModel(tableModel);
	}
	
	@Override
    public String getStaticLabel() {//设置显示名称
        return SAMPLER_NAME;
    }
	
	@Override
	public void clearGui() {
		super.clearGui();
		ipText.setText("");
		portText.setText("");
		infnameText.setText("");
		methodText.setText("");
		textArea.setText("");
		impClass.setText("Telnet Client");
		initTableData();
		jTabbedpane.setSelectedIndex(0);
		timeoutText.setText("");
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==templeteBtn){
			String infName = infnameText.getText();
			if(infName==null || "".equals(infName)){
				JOptionPane.showMessageDialog(this, "接口名称不能为空");
				infnameText.grabFocus();
				return;
			}
			String method = methodText.getText();
			if(method==null || "".equals(method)){
				JOptionPane.showMessageDialog(this, "方法名称不能为空");
				methodText.grabFocus();
				return;
			}
			try{
				if(!"".equals(textArea.getText().trim())){
					int opt = JOptionPane.showConfirmDialog(this, 
							"参数输入框不为空，是否确定用模板参数替换当前参数？", null, JOptionPane.YES_NO_OPTION);
					if(opt!=0){
						//logger.info("======= here will throw a test exception ======");
						//throw new IllegalArgumentException("test exception");
						return;
					}
				}
				//logger.info("======= here will throw a test error ======");
				TempletHelper helper = new TempletHelper(infName, method);
				textArea.setText(helper.getTemplet());
			}catch(Exception ce){
				textArea.setText(ce.getClass().getName() + ": " + ce.getMessage());
			}
		
		}
	}

}
