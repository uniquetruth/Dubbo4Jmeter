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
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

public class DubboSamplerGUI extends AbstractSamplerGui {
	
	private final JLabeledTextField ipText = new JLabeledTextField("ip :");
	private final JLabeledTextField portText = new JLabeledTextField("port :");
	private final JLabeledTextField infnameText = new JLabeledTextField("interface name :");
	private final JLabeledTextField methodText = new JLabeledTextField("invoke method :");
	private final JSyntaxTextArea textArea = JSyntaxTextArea.getInstance(50, 50);
	private final JButton beautifyBtn = new JButton("beautify");
	private final String[] impChoice = {"Telnet Client", "Generic Service"};
	private final JLabeledChoice impClass = new JLabeledChoice("implement :",impChoice);
	private final String SAMPLER_NAME = "Dubbo Sampler";
	private final JLabeledTextField argsTypeText = new JLabeledTextField("arguments type :");
	private final JLabeledTextField timeoutText = new JLabeledTextField("timeout(milliseconds) :");

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
		gbl_header.rowWeights = new double[]{1, 1, 1, 1};
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
		gbc_methodText.gridwidth = 3;
		gbc_methodText.gridx = 1;
		gbc_methodText.gridy = 2;
		header.add(methodText, gbc_methodText);
		
		impClass.setPreferredSize(new Dimension(250, 32));
		GridBagConstraints gbc_acField = new GridBagConstraints();
		gbc_acField.anchor = GridBagConstraints.WEST;
		gbc_acField.insets = new Insets(-5, 45, 5, 5);
		gbc_acField.gridx = 4;
		gbc_acField.gridy = 2;
		header.add(impClass, gbc_acField);
		impClass.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if(impClass.getText().equals("Telnet Client")){
					argsTypeText.setEnabled(false);
				}else{
					argsTypeText.setEnabled(true);
				}
				
			}
		});
		
		argsTypeText.setEnabled(false);
		GridBagConstraints gbc_argsType = new GridBagConstraints();
		gbc_argsType.fill = GridBagConstraints.BOTH;
		gbc_argsType.insets = new Insets(0, 0, 5, 5);
		gbc_argsType.gridwidth = 3;
		gbc_argsType.gridx = 1;
		gbc_argsType.gridy = 3;
		header.add(argsTypeText, gbc_argsType);
		
		GridBagConstraints gbc_Timeout = new GridBagConstraints();
		gbc_Timeout.fill = GridBagConstraints.BOTH;
		gbc_Timeout.insets = new Insets(0, 0, 5, 5);
		gbc_Timeout.gridx = 4;
		gbc_Timeout.gridy = 3;
		header.add(timeoutText, gbc_Timeout);
		
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
		
		JPanel textP = new VerticalPanel(1, 0.1f);
		textP.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), "Arguments"));
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		JTextScrollPane tapanel = JTextScrollPane.getInstance(textArea);
		JPanel bp = new JPanel();
		bp.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
		bp.add(beautifyBtn);
		textP.add(bp);
		textP.add(tapanel);
		mainPanel.add(textP, BorderLayout.CENTER);
		
		setName(SAMPLER_NAME);
	}

	public TestElement createTestElement() {
		DubboSampler ds = new DubboSampler();
		modifyTestElement(ds);
		return ds;
	}

	public String getLabelResource() {
		//not been called yet
		return this.getClass().getSimpleName();
	}

	//data:from gui to sampler
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
		argsTypeText.setText(ds.getArgsType());
		timeoutText.setText(ds.getTimeout());
	}
	
	private void setupSamplerProperties(DubboSampler sampler){
		sampler.setIp(ipText.getText().trim());
		sampler.setPort(portText.getText().trim());
		sampler.setInfName(infnameText.getText().trim());
		sampler.setInvokeCMD(textArea.getText());
		sampler.setImpClass(impClass.getText());
		sampler.setMethod(methodText.getText().trim());
		sampler.setArgsType(argsTypeText.getText());
		sampler.setTimeout(timeoutText.getText());
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
		argsTypeText.setText("");
		timeoutText.setText("");
	}

}
