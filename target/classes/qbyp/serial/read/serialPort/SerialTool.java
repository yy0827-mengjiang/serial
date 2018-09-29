package qbyp.serial.read.serialPort;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;

import qbyp.serial.read.sendsms.Yunxin;
import qbyp.serial.read.serialException.NoSuchPort;
import qbyp.serial.read.serialException.NotASerialPort;
import qbyp.serial.read.serialException.PortInUse;
import qbyp.serial.read.serialException.ReadDataFromSerialPortFailure;
import qbyp.serial.read.serialException.SendDataToSerialPortFailure;
import qbyp.serial.read.serialException.SerialPortInputStreamCloseFailure;
import qbyp.serial.read.serialException.SerialPortOutputStreamCloseFailure;
import qbyp.serial.read.serialException.SerialPortParameterFailure;
import qbyp.serial.read.serialException.TooManyListeners;
import qbyp.serial.read.util.ConnectionPool4MySql;
import qbyp.serial.read.util.PropertiesReader;
import qbyp.serial.read.util.UUIDUtil;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 串口服务类，提供打开、关闭串口，读取、发送串口数据等服务（采用单例设计模式）
 *
 */
public class SerialTool {

	private static Logger logger = Logger.getLogger(SerialTool.class);

	private static SerialTool serialTool = null;

	private static SerialPort serialPort = null;

	private static String bytes_cache = null;

	static {
		// 在该类被ClassLoader加载时就初始化一个SerialTool对象
		if (serialTool == null) {
			serialTool = new SerialTool();
		}
	}

	// 私有化SerialTool类的构造方法，不允许其他类生成SerialTool对象
	private SerialTool() {
	}

	/**
	 * 获取提供服务的SerialTool对象
	 * 
	 * @return serialTool
	 */
	public static SerialTool getSerialTool() {
		if (serialTool == null) {
			serialTool = new SerialTool();
		}
		return serialTool;
	}

	public static SerialPort getSerialPort(String portName, int baudrate) {
		try {
			if (serialPort == null) {
				serialPort = openPort(portName, baudrate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return serialPort;
	}

	/**
	 * 查找所有可用端口
	 * 
	 * @return 可用端口名称列表
	 */
	public static final ArrayList<String> findPort() {

		// 获得当前所有可用串口
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier
				.getPortIdentifiers();

		ArrayList<String> portNameList = new ArrayList<String>();

		// 将可用串口名添加到List并返回该List
		while (portList.hasMoreElements()) {
			String portName = portList.nextElement().getName();
			portNameList.add(portName);
		}

		return portNameList;

	}

	/**
	 * 打开串口
	 * 
	 * @param portName
	 *            端口名称
	 * @param baudrate
	 *            波特率
	 * @return 串口对象
	 * @throws SerialPortParameterFailure
	 *             设置串口参数失败
	 * @throws NotASerialPort
	 *             端口指向设备不是串口类型
	 * @throws NoSuchPort
	 *             没有该端口对应的串口设备
	 * @throws PortInUse
	 *             端口已被占用
	 */
	public static final SerialPort openPort(String portName, int baudrate)
			throws SerialPortParameterFailure, NotASerialPort, NoSuchPort,
			PortInUse {

		try {

			// 通过端口名识别端口
			CommPortIdentifier portIdentifier = CommPortIdentifier
					.getPortIdentifier(portName);

			// 打开端口，并给端口名字和一个timeout（打开操作的超时时间）
			CommPort commPort = portIdentifier.open(portName, 2000);

			// 判断是不是串口
			if (commPort instanceof SerialPort) {

				SerialPort serialPort_open = (SerialPort) commPort;

				try {
					// 设置一下串口的波特率等参数
					serialPort_open.setSerialPortParams(baudrate,
							SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);
				} catch (UnsupportedCommOperationException e) {
					throw new SerialPortParameterFailure();
				}

				// System.out.println("Open " + portName + " sucessfully !");
				serialPort = serialPort_open;
				return serialPort_open;

			} else {
				// 不是串口
				throw new NotASerialPort();
			}
		} catch (NoSuchPortException e1) {
			throw new NoSuchPort();
		} catch (PortInUseException e2) {
			throw new PortInUse();
		}
	}

	/**
	 * 关闭串口
	 * 
	 * @param serialport
	 *            待关闭的串口对象
	 */
	public static void closePort(SerialPort serialPort) {
		if (serialPort != null) {
			serialPort.close();
			serialPort = null;
		}
	}

	/**
	 * 往串口发送数据
	 * 
	 * @param serialPort
	 *            串口对象
	 * @param order
	 *            待发送数据
	 * @throws SendDataToSerialPortFailure
	 *             向串口发送数据失败
	 * @throws SerialPortOutputStreamCloseFailure
	 *             关闭串口对象的输出流出错
	 */
	public static void sendToPort(SerialPort serialPort, byte[] order)
			throws SendDataToSerialPortFailure,
			SerialPortOutputStreamCloseFailure {

		OutputStream out = null;

		try {

			out = serialPort.getOutputStream();
			out.write(order);
			// out.flush();

		} catch (IOException e) {
			throw new SendDataToSerialPortFailure();
		} finally {
			try {
				if (out != null) {
					out.close();
					out = null;
				}
			} catch (IOException e) {
				throw new SerialPortOutputStreamCloseFailure();
			}
		}

	}

	/**
	 * 从串口读取数据
	 * 
	 * @param serialPort
	 *            当前已建立连接的SerialPort对象
	 * @return 读取到的数据
	 * @throws ReadDataFromSerialPortFailure
	 *             从串口读取数据时出错
	 * @throws SerialPortInputStreamCloseFailure
	 *             关闭串口对象输入流出错
	 */
	public static byte[] readFromPort(SerialPort serialPort)
			throws ReadDataFromSerialPortFailure,
			SerialPortInputStreamCloseFailure {

		InputStream in = null;
		byte[] bytes = null;

		try {

			in = serialPort.getInputStream();
			int bufflenth = in.available(); // 获取buffer里的数据长度

			while (bufflenth != 0) {
				bytes = new byte[bufflenth]; // 初始化byte数组为buffer中数据的长度
				in.read(bytes);
				bufflenth = in.available();
			}
		} catch (IOException e) {
			throw new ReadDataFromSerialPortFailure();
		} finally {
			try {
				if (in != null) {
					in.close();
					in = null;
				}
			} catch (IOException e) {
				throw new SerialPortInputStreamCloseFailure();
			}

		}

		return bytes;

	}

	/**
	 * 从串口读取数据
	 * 
	 * @param serialPort
	 *            当前已建立连接的SerialPort对象
	 * @return 读取到的数据
	 * @throws ReadDataFromSerialPortFailure
	 *             从串口读取数据时出错
	 * @throws SerialPortInputStreamCloseFailure
	 *             关闭串口对象输入流出错
	 */
	public static void readFromPort_Analysis(SerialPort serialPort)
			throws ReadDataFromSerialPortFailure,
			SerialPortInputStreamCloseFailure {

		InputStream in = null;
		// byte[] bytes = null;
		logger.info("进入readFromPort_Analysis！");
		try {
			in = serialPort.getInputStream();
			byte[] bytes2 = new byte[1024];
			int bufflenth = in.read(bytes2); // 获取buffer里的数据长度
			String bytes_string = "";
			while (bufflenth != -1) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String byte2String = new String(bytes2).trim();
				if (bufflenth < 1024) {
					if (byte2String.length() > 1) {
						byte2String = byte2String.substring(0, bufflenth);
					}
				}
				bytes_string = bytes_string + byte2String;
				// System.out.println("bytes_string");
				if (!bytes_string.equals("")) {
					// 40402323 开始标识
					// 24242525 结束标识
					// String bytes_string=readStr;
					if (bytes_cache != null && bytes_cache.length() > 0) {
						// System.out.println("bytes_cache:"+new
						// String(bytes_cache));
						bytes_string = bytes_cache + bytes_string;
					}
					if (bytes_string.indexOf("40402323") != -1) {
						// System.out.println("1");
						if (bytes_string.indexOf("24242525") != -1) {
							// System.out.println("2");
							String[] nobegins = bytes_string.split("40402323");
							if (bytes_string.endsWith("40402323")) {
								// System.out.println("3");
								for (int i = 0; i < nobegins.length; i++) {
									String nobegin = nobegins[i];
									if (nobegin.endsWith("24242525")) {
										String noend = nobegin.substring(0,
												nobegin.length() - 8);
//										logger.info("noend:"
//												+ new String(
//														hexStringToBytes(noend)));
										 SerialTool.jsonToSendMessage(noend);
									}
								}
								bytes_cache = "40402323";
							} else {
								// System.out.println("4");
								for (int i = 0; i < nobegins.length; i++) {
									String nobegin = nobegins[i];
									if (nobegin.endsWith("24242525")) {
										// System.out.println("5");
										String noend = nobegin.substring(0,
												nobegin.length() - 8);
//										logger.info("noend:"
//												+ new String(
//														hexStringToBytes(noend)));
										 SerialTool.jsonToSendMessage(noend);
									} else {
										// System.out.println("6");
										if (i == (nobegins.length - 1)) {
											nobegin = "40402323" + nobegin;
											// System.out.println("nobegin:"+nobegin);
											bytes_cache = nobegin;
										}
									}
								}
							}

						} else {
							// 没有结束标识，直接放到缓存bytes_cache
							// System.out.println("没有结束:"+bytes_string);
							bytes_cache = bytes_string;
							//logger.info("bytes_cache1:" + bytes_cache);
						}
					} else {// 没有开始标识，直接放到缓存bytes_cache
							// System.out.println("没有开始:"+bytes_string);
						bytes_cache = bytes_string;
						//logger.info("bytes_cache2:" + bytes_cache);
					}
				}
				bytes_string = "";
				bufflenth = in.read(bytes2);
			}
		} catch (IOException e) {
			throw new ReadDataFromSerialPortFailure();
		} finally {
			try {
				if (in != null) {
					in.close();
					in = null;
				}
			} catch (IOException e) {
				throw new SerialPortInputStreamCloseFailure();
			}

		}

	}

	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	/**
	 * 添加监听器
	 * 
	 * @param port
	 *            串口对象
	 * @param listener
	 *            串口监听器
	 * @throws TooManyListeners
	 *             监听类对象过多
	 */
	public static void addListener(SerialPort port,
			SerialPortEventListener listener) throws TooManyListeners {

		try {

			// 给串口添加监听器
			port.addEventListener(listener);
			// 设置当有数据到达时唤醒监听接收线程
			port.notifyOnDataAvailable(true);
			// 设置当通信中断时唤醒中断线程
			port.notifyOnBreakInterrupt(true);

		} catch (TooManyListenersException e) {
			throw new TooManyListeners();
		}
	}

	public static void jsonToSendMessage(String byteStr) {
		if (byteStr != null && byteStr.length() > 0) {
			String jsonString = new String(
					SerialTool.hexStringToBytes(new String(byteStr)));
			logger.info("读取串口信息：" + jsonString);
			if (jsonString != null) {
				try {
					JSONObject json = JSONObject.parseObject(jsonString);
					String tel = json.getString("tel");
					String message = json.getString("message");
					String ewType = json.getString("ewType");
					String params_str = json.getString("params");
					JSONArray params = JSONArray.parseArray(params_str);

					String sms_switch = PropertiesReader
							.getProperty("sms_switch");
					String is_success = "success";
					if (sms_switch.equals("1")) {
						is_success = Yunxin.sendMsg(tel, params, ewType);
					}
					if (is_success.equals("success")) {
						is_success = "1";
					} else {
						is_success = "0";
					}
					String uuid = UUIDUtil.generateUUIDString();
					String sql = " insert into T_SMS_LOG(ID,TEL,MSG,SUCCESS,SENDTIME) VALUES ('"
							+ uuid
							+ "','"
							+ tel
							+ "','"
							+ message
							+ "',"
							+ is_success + ",NOW())";
					boolean success = ConnectionPool4MySql.getInstance()
							.update(sql);
					if (!success) {
						logger.error("号码：" + tel + " 短信记录日志入库失败！");
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
	}

}
