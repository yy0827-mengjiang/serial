package qbyp.serial.read;

import gnu.io.SerialPort;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import qbyp.serial.read.serialPort.SerialTool;
import qbyp.serial.read.util.PathUtil;
import qbyp.serial.read.util.PropertiesReader;

/**
 * Hello world!
 *
 */
public class App {
	private static Logger logger = Logger.getLogger(App.class);
	private static String LOG4J_PROPERTIES = "log4j.properties";

	/**
	 * 读取并发送
	 * 
	 * @author yangyi
	 *
	 */
	public static class ReadAndSendThread extends Thread {

		public ReadAndSendThread() {

		}

		@Override
		public void run() {
			logger.info("读取启动！");
			while (true) {
				try {
					SerialPort serialPort = SerialTool.getSerialPort(
							PropertiesReader.getProperty("portName"), Integer
									.valueOf(PropertiesReader
											.getProperty("baudrate")));
					// byte[] byteStr = SerialTool.readFromPort(serialPort);
					SerialTool.readFromPort_Analysis(serialPort);
					// String frequency = PropertiesReader
					// .getProperty("frequency");
					// Thread.sleep(Integer.valueOf(frequency));
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("出错", e);
				}
			}

		}
	}

	public static void main(String[] args) throws Exception {
		PropertyConfigurator
				.configure(PathUtil.getConfigPath(LOG4J_PROPERTIES));
		// String inputType="check";
		ReadAndSendThread readAndSendThread = new ReadAndSendThread();
		readAndSendThread.start();

	}

	public static void main2(String[] args) {
		String ss = "{\"ewType\":\"CIRCLE\",\"ringId\":\"EyGthhGqjcoa1SD1LUvviD\",\"tel\":\"13422361141\",\"message\":\"车辆防盗提醒：李爷，您的“粤A22222”车辆,已离开防盗区域，防盗标签供电正常，目前进入“豪居酒家6”，地址：“梅州市平远县仁居镇仁居村老东学前街40号”，谨防被盗，如有被盗情况，请及时拨打110报警，谢谢！ \",\"params\":\"[\\\"李爷\\\",\\\"粤A22222\\\",\\\"正常\\\",\\\"豪居酒家6\\\",\\\"梅州市平远县仁居镇仁居村老东学前街40号\\\"]\"}{\"ewType\":\"CIRCLE\",\"ringId\":\"UywpqGRcKHvTVMKQDyVTvF\",\"tel\":\"18902284520\",\"message\":\"车辆防盗提醒：李爷，您的“粤A33333”车辆,已离开防盗区域，防盗标签供电正常，目前进入“豪居酒家6”，地址：“梅州市平远县仁居镇仁居村老东学前街40号”，谨防被盗，如有被盗情况，请及时拨打110报警，谢谢！ \",\"params\":\"[\\\"李爷\\\",\\\"粤A33333\\\",\\\"正常\\\",\\\"豪居酒家6\\\",\\\"梅州市平远县仁居镇仁居村老东学前街40号\\\"]\"}";
		if (ss.indexOf("}{") != -1) {
			String[] ssss = ss.split("\\}\\{");
			for (int i = 0; i < ssss.length; i++) {
				String jsonStr = ssss[i];
				if (!jsonStr.startsWith("{")) {
					jsonStr = "{" + jsonStr;
				}
				if (!jsonStr.endsWith("}")) {
					jsonStr = jsonStr + "}";
				}
				System.out.println(jsonStr);
			}
		}
	}

}
