/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package serialcommunication.example;

import gnu.io.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;
import javax.swing.JOptionPane;
import serialcommunication.userinterface.GUI;

/**
 *
 * @author Rizaldi Habibie
 */
public class Example implements Runnable, SerialPortEventListener{
    
    GUI window = null;
    //for saving ports that will be foud
    private Enumeration ports = null;
    //map the port names to CommPortIndentifiers
    private HashMap portMap = new HashMap();
    //this is the object that contains the opened port
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort;
    
    //input and output streams for sending and receiving data
    private InputStream input = null;
    private OutputStream output = null;
    
    //flag if the program is connected to port serial or not
    private boolean connected = false, showInput = false;
    
    //timeout for connecting port
    private final static int TIMEOUT = 2000;
    
    //some ascii value for certain things
    private final static int SPACE_ASCII = 32;
    private final static int DASH_ASCII = 45;
    private final static int NEW_LINE_ASCII = 10;
    
    private int loop = 0;
    private boolean getReady = false;
    private String activeRegister, temper, status;
    private String selectedPort ;
    
    //as string for recording what goes on in the program
    //this string is written to the gui
    private String logText = "";
    private Thread readThread;
    private byte[] readBuffer;
    public Example(GUI window) {
        this.window = window;
    }
    
    public void searchAvailablePorts(){
        ports = CommPortIdentifier.getPortIdentifiers();
        while(ports.hasMoreElements()){
            CommPortIdentifier currentPort = (CommPortIdentifier)ports.nextElement();
            //get only serial port
            if(currentPort.getPortType() == CommPortIdentifier.PORT_SERIAL){
                window.getPortCbx().addItem(currentPort.getName());
                portMap.put(currentPort.getName(), currentPort);
            }
        }
    }
    
    public void connect(){
        selectedPort = (String)window.getPortCbx().getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);
        CommPort commPort=null;
        try{
            commPort = selectedPortIdentifier.open("Control Panel", TIMEOUT);
            serialPort = (SerialPort)commPort;
            setConnected(true);
            logText = selectedPort+" Opened Successfully";
            window.getTextArea().setForeground(Color.black);
            window.getTextArea().append(logText+"\n");
           
        }catch(PortInUseException e){
            logText = selectedPort + " is in use. (" + e.toString() + ")";
            window.getTextArea().setForeground(Color.RED);
            window.getTextArea().append(logText + "\n");
        }catch(Exception e){
            logText = "Failed to open " + selectedPort + "(" + e + ")";
            e.printStackTrace();
            window.getTextArea().append(logText + "\n");
            window.getTextArea().setForeground(Color.RED);
        }
    }
    
    public boolean initIOStream(){
        boolean successful = false;
        try{
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();
//            writeData(0,0);
            successful = true;
        }catch(IOException e){
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
            window.getTextArea().setForeground(Color.red);
            window.getTextArea().append(logText + "\n");
            return successful;
        }
        return successful;
    }
    public void initListener(){
        try{
           serialPort.addEventListener(this);
           serialPort.notifyOnDataAvailable(true);  
           serialPort.setSerialPortParams(4800, SerialPort.DATABITS_8,SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
           readThread = new Thread(this);
           readThread.start();
        }catch(TooManyListenersException e){
            logText = "Too many listeners. (" + e.toString() + ")";
            window.getTextArea().setForeground(Color.red);
            window.getTextArea().append(logText + "\n");
        } catch (UnsupportedCommOperationException e) {
            logText = "error (" + e.toString() + ")";
            window.getTextArea().setForeground(Color.red);
            window.getTextArea().append(logText + "\n");
        }
    }
    public boolean showInput(){
        return showInput = true;
    }
    public boolean dontShowInput(){
        return showInput = false;
    }
    public void disconnect(){
        try{
            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
//            window.keybindingController.toggleControls();
            logText = "Disconnected.";
            window.getTextArea().setForeground(Color.red);
            window.getTextArea().append(logText + "\n");
        }catch(Exception e){
            logText = "Failed to close " + serialPort.getName()
                              + "(" + e.toString() + ")";
            window.getTextArea().setForeground(Color.red);
            window.getTextArea().append(logText + "\n");
        }
    }
    @Override
    public void serialEvent(SerialPortEvent event) {
        if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE){
            readBuffer = new byte[20];
            try{
                byte singleData = (byte)input.read();
                if(singleData != NEW_LINE_ASCII){
                    logText = new String(new byte[] {singleData});
                    String data = logText+"\n";
                    if(showInput){
                       window.getTextArea().append(data);
                    }else{
                       window.getTextArea().setText("Connected to port " + selectedPort+"\nRetrieving data..."); 
                    }
                    if(logText.contains("#")){
                        temper = "";
                        getReady = true;
                    }
                    if(getReady){
                        loop++;
                        if(loop==2 && logText.contains("3")){
                             activeRegister = "control room";
                        }else if(loop==2 && logText.contains("4")){
                            activeRegister = "sheet cutter";
                        }else if(loop==2 && logText.contains("5")){
                            activeRegister = "stamping";
                        }else if(loop==2 && logText.contains("6")){
                           activeRegister = "sensitizing";
                        }else if(loop==2 && logText.contains("7")){
                            activeRegister = "lorong bawah";
                        }else if(loop==2 && logText.contains("8")){
                            activeRegister = "sortir";
                        }else if(loop==2 && logText.contains("9")){
                            activeRegister = "laborat";
                        }else if(loop==2 && logText.contains(":")){
                             activeRegister = "hr";
                        }else if(loop==2 && logText.contains(";")){
                             activeRegister = "peruri";
                        }else if(loop==2 && logText.contains("<")){
                             activeRegister = "lorong atas";
                        }
                        
                        if(loop == 3){
                            if(logText.contains("O")){
                                status = "OK";
                            }else if(logText.contains("P")){
                                status = "HOT !!";
                            }else{
                                status = "Error"+logText;
                            }
                        }
                        if (loop==4 || loop==5){
                            temper = temper +""+logText;
                        }
                        if(loop ==5){
                            if(activeRegister.equals("control room")){
                                window.getControlRoom().setText(temper);
                                window.getStatusControlRoom().setText(status);
                            }else if(activeRegister.equals("sheet cutter")){
                                window.getSheetCutter().setText(temper);
                                window.getStatusSheetCutter().setText(status);
                            }else if(activeRegister.equals("stamping")){
                                window.getStamping().setText(temper);
                                window.getStatusStamping().setText(status);
                            }else if(activeRegister.equals("sensitizing")){
                                window.getSensitizing().setText(temper);
                                window.getStatusSensitizing().setText(status);
                            }else if(activeRegister.equals("lorong bawah")){
                                window.getLorongBawah().setText(temper);
                                window.getStatusLorongBawah().setText(status);
                            }else if(activeRegister.equals("sortir")){
                                window.getSortir().setText(temper);
                                window.getStatusSortir().setText(status);
                            }else if(activeRegister.equals("laborat")){
                                window.getLaborat().setText(temper);
                                window.getStatusLaborat().setText(status);
                            }else if(activeRegister.equals("hr")){
                                window.getHr().setText(temper);
                                window.getStatusHr().setText(status);
                            }else if(activeRegister.equals("peruri")){
                                window.getPeruri().setText(temper);
                                window.getStatusPeruri().setText(status);
                            }else if(activeRegister.equals("lorong atas")){
                                window.getLorongAtas().setText(temper);
                                window.getStatusLorongAtas().setText(status);
                            }
                            activeRegister = "";
                            loop = 0;
                            getReady = false;
                                
                        }
//                        if(loop == 2 && logText.contains("9")){
//                            try{
//                                System.out.println(suhuRata);
//                                suhuRata = suhuRata/10;
//                                window.getShowTemp().setText("" + suhuRata );
//                                suhuRata = 0;
//                            }catch(Exception e){
//                                JOptionPane.showMessageDialog(null,"Error"+e);
//                            }
//                           
//                        }
                    }
                    
                }
//                int available = input.available();
//                if(available>0){
//                    input.read(readBuffer,0,available);
//                    logText = new String(readBuffer,0,available)+"\n";
//                        System.out.println(available);
//                    window.getTextArea().append(logText);
//                }
            }catch(Exception e){
                logText = "Failed to read data. (" + e.toString() + ")";
                window.getTextArea().setForeground(Color.red);
                window.getTextArea().append(logText + "\n");
            }
        }
//         switch(event.getEventType()) {
//            case SerialPortEvent.BI:
//            case SerialPortEvent.OE:
//            case SerialPortEvent.FE:
//            case SerialPortEvent.PE:
//            case SerialPortEvent.CD:
//            case SerialPortEvent.CTS:
//            case SerialPortEvent.DSR:
//            case SerialPortEvent.RI:
//            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
//             System.out.print("empty");
//            case SerialPortEvent.DATA_AVAILABLE:
//                byte[] readBuffer = new byte[20];
//                int numBytes = 0;
//            try {
//                while (input.available() > 0) {
//                    numBytes = input.read(readBuffer);
//                }
//                 logText = ""+new String(readBuffer)+"\n";
//                 window.getTextArea().append(logText);
//            } catch (IOException e) {
//                System.out.println(e);
//            }
//                break;
//        }
    
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
//    public void writeData(int leftThrottle, int rightThrottle)
//    {
//        try
//        {
//            output.write(leftThrottle);
//            output.flush();
//            //this is a delimiter for the data
//            output.write(DASH_ASCII);
//            output.flush();
//            
//            output.write(rightThrottle);
//            output.flush();
//            //will be read as a byte so it is a space key
//            output.write(SPACE_ASCII);
//            output.flush();
//        }
//        catch (Exception e)
//        {
//            logText = "Failed to write data. (" + e.toString() + ")";
//            window.getTextArea().setForeground(Color.red);
//            window.getTextArea().append(logText + "\n");
//        }
//    }

    @Override
    public void run() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {System.out.println(e);}
    }

}
