


import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
public class SmartGridConsumer {



	private static String host1 = "localhost";
    private static String host2 = "localhost";


    int currentConsumer;
    private static int myConsumerNum = 0; // 0 or 1 or 2
    String token;
    
    //ports and sockets
    /*-----changes for each client----------*/
    int port0;
    int port1;
    int port2;
    
    ServerSocket serverSocket; //servers socket
    Socket s;   //server's connection socket
    Socket s1 ;     //client socket to other server 1
    Socket s2;  //client socket to other server 2
    
    //readers and Strings
    BufferedReader br =null;
    String oldfilename = "consumer.txt";
    String newFilename ="new.txt";
    String currentLine;

    File myFile = new File ("consumer.txt");
    
    // lists used in the program
    List<String> file1 = new ArrayList<String>(); //holds values from server 1's file
    List <String> file2 = new ArrayList<String>(); //holds values from server 2's file
    List <String> myValues = new ArrayList<String>(); //holds my values which is to be modified and written to my file
    
    // true when i am server
    boolean serverOn = false;
    

    // variables used in calculations
    private double systemAverage;
    private double systemPeak;
    private double systemVariance;
    private int systemPeakHour;
    private double PAR; // peak to average ratio
    private ArrayList < String > inflexibleAppliances;
    private ArrayList < String > flexibleAppliances;
    private static int numberOfHoursInADay = 24;
    
    private double myFlexibleAppliance1Consumption;
    private double myFlexibleAppliance2Consumption;

    private int myFlexibleAppliance1HoursNeeded;
    private int myFlexibleAppliance2HoursNeeded;

    private int myFlexibleAppliance1StartingHour;
    private int myFlexibleAppliance2StartingHour;

    private int myFlexibleAppliance1EndingHour;
    private int myFlexibleAppliance2EndingHour;

    private int myFlexibleAppliance1CorrectStartingHour; 
    private int myFlexibleAppliance2CorrectStartingHour;

    private int myFlexibleAppliance1LowestStartingHour; // the time at which it should start so that PAR is minimum
    private int myFlexibleAppliance2LowestStartingHour;


    private double minimumSystemPAR;

    // These 2 strings will be obtained from the others
    // these are just default values
    private static String consumption0 = new String("2.01 1.76 3.76 3.76 3.76 3.76 5.26 4.81 0.56 0.26 0.16 0.16 0.16 0.16 0.16 0.16 0.16 1.76 2.06 0.56 0.71 0.71 2.31 4.81");
    private static String consumption1 = new String("2.01 1.76 3.76 3.76 3.76 3.76 5.26 4.81 0.56 2.06 1.96 0.16 0.16 0.16 0.16 0.16 0.16 1.76 2.06 0.56 0.71 0.71 2.31 4.81");

    private String myConsumption;
    private double[] myConsumptionArray;
    private double[] myConsumptionArrayWithoutFlexible;
    private double[] myLowestConsumptionArray;

    private Integer ifOneReduced=1; 
    private Integer ifTwoReduced=1; 
    private Integer ifMyReduced=1; 
    boolean endIteration = false;
    boolean quitApplication = false;
    
    public SmartGridConsumer(){

    port0 = 9000; // my port
    port1 = 9001;
    port2 = 9002;
    
    
    systemAverage = 0.0;
    systemPeak = 0.0;
    systemPeakHour = 0;
    PAR = 1000000;
    systemVariance = 0.0;

    myConsumptionArray = new double[numberOfHoursInADay];
    myLowestConsumptionArray = new double[numberOfHoursInADay];
    myConsumptionArrayWithoutFlexible = new double[numberOfHoursInADay];
    inflexibleAppliances = new ArrayList < String > ();
    flexibleAppliances = new ArrayList < String > ();


    myFlexibleAppliance1Consumption = 0.0;
    myFlexibleAppliance2Consumption = 0.0;

    myFlexibleAppliance1HoursNeeded = 0;
    myFlexibleAppliance2HoursNeeded = 0;

    myFlexibleAppliance1StartingHour = 0;
    myFlexibleAppliance2StartingHour = 0;

    myFlexibleAppliance1EndingHour = 0;
    myFlexibleAppliance2EndingHour = 0;

    myFlexibleAppliance1CorrectStartingHour = -1;
    myFlexibleAppliance2CorrectStartingHour = -1;

    myFlexibleAppliance1LowestStartingHour = -1;
    myFlexibleAppliance2LowestStartingHour = -1;
    minimumSystemPAR=0.0;
    
        
}

public static void main(String[] args) throws Exception
{

	SmartGridConsumer consumer = new SmartGridConsumer();
	consumer.populateInflexibleAppliances(); // first populate inflexible appliances
	consumer.updateMyConsumptionWithInflexibleAppliances(); // then put them into the array
	consumer.setMyFlexibleApplianceDetails(); // next comes the flexible appliances
	consumer.addMyFlexibleAppliancesToConsumption(); // add them into the array
	consumer.computeVariance();
	consumer.start();
}

//decides to run client or server depeding on a round robin logic indicated by number in my file
public void start()  throws Exception
{
      serverSocket = new ServerSocket(port0); //Welcome socket
      

      try{

      	br = new BufferedReader(new FileReader( oldfilename));
      	if((currentLine = br.readLine()) != null)
      	{

      		currentConsumer = currentLine.charAt(0) - '0';
      	}
      	else
      	{
      		System.out.println("No text found in file");
      	}

      	if(currentConsumer == myConsumerNum) // this means it is my turn
      	{
      		serverOn = false;
            new client().start(); // client
      	}
      	else
      	{ 
      		serverOn = true;
      	}
      	runServer();
      }
      catch(IOException ie) 
      {
      	ie.printStackTrace();
      }
      
  }

  public class client extends Thread 
  {

  	public client() 
  	{
  		super("client"); // needed as first line in the constucture

  	}

  	public void run() 
  	{   try {


  		boolean scanning=true;
  		while(scanning)
  		{
  			try
  			{
                s1 = new Socket(host1, port1); // found a socket
  				scanning=false; // so no need to scan again
  			}
  			catch(Exception e)
  			{
  				System.out.println("Connect failed, waiting and trying again");
  				try
  				{
                        Thread.sleep(2000);//2 seconds
                    }
                    catch(InterruptedException ie){
                    	ie.printStackTrace();
                    }
                }
            }

            boolean scanning2=true;
            while(scanning2)
            {
            	try
            	{
                  

            		s2 = new Socket(host2, port2);
            		scanning2=false;
            	}
            	catch(Exception e)
            	{
            		System.out.println("Connect failed, waiting and trying again");
            		try
            		{
                        Thread.sleep(2000);//2 seconds
                    }
                    catch(InterruptedException ie){
                    	ie.printStackTrace();
                    }
                } 
            }

          getFile1(); //fetch and read file from server 1
          getFile2(); //fetch and read file from server 2
          makeCalculations(); //make calculations and change myValues
          modifyFile();  //modify own file by adding in myValues
          passToken();//pass on consumer token
      } catch (Exception ex) {
      	Logger.getLogger(SmartGridConsumer.class.getName()).log(Level.SEVERE, null, ex);
      }

  }  
}


    //repeated checking if it has to change from server to client
public void checkConsumer() throws Exception
{
	try{
		br = new BufferedReader(new FileReader( oldfilename));
		if((currentLine = br.readLine()) != null)
		{
			currentConsumer = currentLine.charAt(0) - '0';
		}
		else
		{
			System.out.println("No text found in file");
		}

    	if(currentConsumer == myConsumerNum)
		{   
			
            
			serverOn = false;
			new client().start();
		}
		else
		{    
			if(serverOn==false) // if it is not on, turn it on
			{ 
				serverOn = true;
				closeIt(s1);
				closeIt(s2);

    
				runServer();
			} 
		}

		}
		catch(IOException ie) 
		{
			ie.printStackTrace();
		}
	}

    //server
	public void runServer() throws Exception
	{
		try 
		{    

     
			while(!quitApplication)
			{
                     s = serverSocket.accept(); //Connection socket 
                     ObjectOutputStream outToClient = new ObjectOutputStream (s.getOutputStream());  
                     sendFileToClient(outToClient);
                      //Read change in token form client
                     BufferedReader inFromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     String clientSentence = inFromClient.readLine();

                     
                     if(clientSentence!=null)
                     {
                     	StringTokenizer st = new StringTokenizer(clientSentence);
                      String message = st.nextToken();
                     	if (message.equals("Token"))
                     	{
                     		token=st.nextToken();
                            //read new token to own file
                     		br = new BufferedReader(new FileReader( oldfilename));
                     		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
                     		String line ;
                     		int i=0;
                     		int lineNum=0;
                     		while((line = br.readLine())!=null )
                     			{     lineNum++;
                     				if(lineNum==1)
                     				{
                     					String l=line.replace(line,token);
                     					bw.write(l+"\n");
                     				}
                     				else
                     				{
                     					bw.write(line+"\n");
                     				}
                     			}
                     			try {
                     				if(br != null)
                     					br.close();
                     			} catch (IOException e) {
                                    
                     			}
                     			try {
                     				if(bw != null)
                     					bw.close();
                     			} catch (IOException e) {
                                    
                     			}
                              // Once everything is complete, delete old file..
                     			File oldFile = new File(oldfilename);
                     			oldFile.delete();

                              // And rename tmp file's name to old file name
                     			File newFile = new File(newFilename);
                     			newFile.renameTo(oldFile);
                     		}
                     		else if(message.equals("QUIT"))
							{
                            System.out.println("Got quit message.Exiting....");
							endIteration =true;
							quitApplication=true;
							closeIt(s1);
							closeIt(s2);
							closeIt(s);
							}


                     	}
                     	checkConsumer();
                     }

                 }
                 catch(IOException ie) 
                 {   
                 	ie.printStackTrace();
                 }


             }

   			 //client

             private void getFile1() throws Exception
             {
             	
             	InputStream in=null;
         		//receive the file
             	if(s1.getInputStream().available()>0) 
             	{
         
             		in = s1.getInputStream();

             		ObjectInputStream ois=new ObjectInputStream(in);
             		File myF=(File)ois.readObject();
             		br = new BufferedReader(new FileReader(myF));
             		String line;
             		int idx=0;
             		file1.clear();
             		while((line = br.readLine())!=null )
             		{ 
             			file1.add(idx++, line);
             		}

           
      }
    
  if(!file1.isEmpty()) { // if file exists
  	consumption0=file1.get(1); 
  	ifOneReduced=Integer.parseInt(file1.get(2)); 
  }
  else
  {
  	 // this is handled by taking default values automatically
  }
    

}
private void getFile2() throws Exception
 {
 	
 	InputStream in=null;
	  //receive the file
 	if(s2.getInputStream().available()>0) 
 	{
	
 		in = s2.getInputStream();

 		ObjectInputStream ois=new ObjectInputStream(in);
 		File myF=(File)ois.readObject();
 		br = new BufferedReader(new FileReader(myF));
 		String line;
 		int idx=0;
 		file2.clear();
 		while((line = br.readLine())!=null )
 		{ 
 			file2.add(idx++, line);
 		}

            
     }
     else
     {
          System.out.println("Input stream  not available");

  	}	
  	if(!file2.isEmpty()) { 
  		consumption1=file2.get(1); 
  		ifTwoReduced=Integer.parseInt(file2.get(2)); 
  	}
  	else
  	{
  		 System.out.println("Taking default values");
  	}
      

}

private void passToken() throws IOException, Exception
{
  
	DataOutputStream outToServer1 = new DataOutputStream( s1.getOutputStream());
	outToServer1.writeBytes("Token "+token+'\n');

	DataOutputStream outToServer2 = new DataOutputStream( s2.getOutputStream());
	outToServer2.writeBytes("Token "+token+'\n');

     

}

private void printMyFile() throws Exception
{

	System.out.println("Printing my file");
	br = new BufferedReader(new FileReader( oldfilename));
	String line ;
	int i=0;
	while((line = br.readLine())!=null )
	{ 
		System.out.println("Line "+i+" "+line);
		i++;
	}
	try {
		if(br != null)
			br.close();
	} catch (IOException e) {
            
	}
}

private void printValues()
{ 
	System.out.println("Printing my values");

	for(int i =0;i<myValues.size();i++)
	{
		System.out.println("Line "+i+" "+myValues.get(i));
	}
}

private void modifyFile() throws IOException, Exception
{

	br = new BufferedReader(new FileReader( oldfilename));
	BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
	String line ;
	int i=0;
	int lineNum=0;
	while((line = br.readLine())!=null )
		{     lineNum++;
			if(lineNum==1)
			{
				String l=line.replace(line,token);
				bw.write(l+"\n");
			}
			else if(lineNum==2)
			{
			
			String l = line.replace(line, myConsumption);
			bw.write(l+"\n");
			}
			else if(lineNum==3)
			{
		
			String l = line.replace(line, ifMyReduced.toString());
			bw.write(l+"\n");
			}
		}

		try {
			if(br != null)
				br.close();
		} catch (IOException e) {
            
		}
		try {
			if(bw != null)
				bw.close();
		} catch (IOException e) {
            
		}
    // Once everything is complete, delete old file..
		File oldFile = new File(oldfilename);
		oldFile.delete();

    // And rename tmp file's name to old file name
		File newFile = new File(newFilename);
		newFile.renameTo(oldFile);

	}
	private void closeIt(Socket soc) //close connection
	{


		try
		{
			soc.close();
		}catch(IOException ie)
		{
			ie.printStackTrace();
		}
	}
	private void sendFileToClient(ObjectOutputStream outToClient) throws Exception
	{
          //sending ASCII text file from server to client side

		myFile = new File ("consumer.txt");
		outToClient.writeObject(myFile);
	}

    //PAR calculations



	private void makeCalculations() throws Exception
	{

		Integer newToken =  (myConsumerNum+1)%3;
		token = newToken.toString();

		System.out.println("Starting search");
		boolean didReduce = exhaustiveSearch();
		if(didReduce) {
			

			System.arraycopy(myLowestConsumptionArray, 0, myConsumptionArray, 0, myLowestConsumptionArray.length);
			copyMyConsumptionFromArrayToString();
			PAR= minimumSystemPAR;
			

		}
		else {
			

			System.out.println("PAR is " + minimumSystemPAR);
			
			ifMyReduced=0; 
			if(ifOneReduced==0&&ifTwoReduced==0&&ifMyReduced==0) { 
				endIteration =true; 
			} 

      
			if(endIteration) 
				{ 
					
					quitApplication=true; 
					DataOutputStream outToServer1 = new DataOutputStream( s1.getOutputStream()); 
					outToServer1.writeBytes("QUIT "+'\n'); 
					DataOutputStream outToServer2 = new DataOutputStream( s2.getOutputStream()); 
					outToServer2.writeBytes("QUIT"+'\n');
          System.exit(0);
				}
		}


		
	}

	

	private void updateMyConsumptionWithInflexibleAppliances() {

		for(int i = 0; i < numberOfHoursInADay; i++)
			myConsumptionArray[i] = 0.0;


		for(int i = 0; i < inflexibleAppliances.size(); i++) {
			int j = 0;
			String s = inflexibleAppliances.get(i);
			for(String temp:s.split("\\s+")) {
				myConsumptionArray[j++] += Double.parseDouble(temp);
			}
		}
		System.arraycopy(myConsumptionArray, 0, myConsumptionArrayWithoutFlexible, 0, myConsumptionArray.length);
		copyMyConsumptionFromArrayToString();
	}

	private void copyMyConsumptionFromArrayToString() {

		StringBuffer tempConsumption = new StringBuffer();

		for(int i = 0; i < numberOfHoursInADay; i++) {
			tempConsumption.append(String.valueOf(myConsumptionArray[i]) + " ");
		}

		myConsumption = tempConsumption.toString();
	}


	private double computePAR() {
		copyMyConsumptionFromArrayToString();
		computeAverage(consumption0, consumption1, myConsumption);
		computePeak(consumption0, consumption1, myConsumption);
		return systemPeak / systemAverage; // this should be set to PAR
	}

	private void setMyFlexibleApplianceDetails() {

		switch(myConsumerNum) {

			case 2: 
			case 0: // Both server 0 and 2 have same appliances

				// Appliance 1 and 2 

				myFlexibleAppliance1Consumption = 2.0; // 2.0 kW
				myFlexibleAppliance2Consumption = 1.8; // 1.8 kW

				myFlexibleAppliance1HoursNeeded = 5; // 5 hours needed
				myFlexibleAppliance2HoursNeeded = 2; // 2 hours needed

				myFlexibleAppliance1StartingHour = 18; // 6pm
				myFlexibleAppliance2StartingHour = 9; // 9am

				myFlexibleAppliance1EndingHour = 7; // 7am
				myFlexibleAppliance2EndingHour = 17; // 5pm

				break;

				case 1: 

				// Appliance 1 and 3

				myFlexibleAppliance1Consumption = 2.0; // 2.0 kW
				myFlexibleAppliance2Consumption = 2.0; // 2.0 kW

				myFlexibleAppliance1HoursNeeded = 5; // 5 hours needed
				myFlexibleAppliance2HoursNeeded = 2; // 2 hours needed

				myFlexibleAppliance1StartingHour = 18; // 6pm
				myFlexibleAppliance2StartingHour = 23; // 1pm

				myFlexibleAppliance1EndingHour = 7; // 7am
				myFlexibleAppliance2EndingHour = 8; // 8am

				break;

				default:

				System.out.println("Unknown server number. Unable to set flexible appliance details");
			}
		}

		private void addMyFlexibleAppliancesToConsumptionGivenStartingPoint(int startingPoint1, int startingPoint2) {

			int tempHourIndex = startingPoint1;
			while(tempHourIndex != (startingPoint1 + myFlexibleAppliance1HoursNeeded) % numberOfHoursInADay) {

				myConsumptionArray[tempHourIndex] += myFlexibleAppliance1Consumption;
				tempHourIndex = (tempHourIndex + 1) % numberOfHoursInADay;
			}

			tempHourIndex = startingPoint2;
			while(tempHourIndex != (startingPoint2 + myFlexibleAppliance2HoursNeeded) % numberOfHoursInADay) {
					myConsumptionArray[tempHourIndex] += myFlexibleAppliance2Consumption;
				tempHourIndex = (tempHourIndex + 1) % numberOfHoursInADay;
			}

			myFlexibleAppliance1CorrectStartingHour = startingPoint1;
			myFlexibleAppliance2CorrectStartingHour = startingPoint2;

			copyMyConsumptionFromArrayToString();
		}

		private void addMyFlexibleAppliancesToConsumption() {

		// add random place for the flexible appliances.
		// in this case, we let them start from the earliest possible

			addMyFlexibleAppliancesToConsumptionGivenStartingPoint(myFlexibleAppliance1StartingHour, myFlexibleAppliance2StartingHour);
		}

private void removeMyFlexibleAppliancesFromConsumption() {

 myFlexibleAppliance1CorrectStartingHour = -1;
 myFlexibleAppliance2CorrectStartingHour = -1;


 System.arraycopy(myConsumptionArrayWithoutFlexible, 0, myConsumptionArray, 0, myConsumptionArrayWithoutFlexible.length);
 copyMyConsumptionFromArrayToString();
}

private boolean isTimeWithinBounds(int start, int end, int toCheck) {
	int temp = start;
	while(temp != (end + 1) % numberOfHoursInADay) {
		if(temp == toCheck)
			return true;
		temp = (temp + 1) % numberOfHoursInADay;
	}
	return false;
}

private boolean isAppliance1WithinBounds(int time) {
	boolean endingPointValid = isTimeWithinBounds(myFlexibleAppliance1StartingHour, 
		myFlexibleAppliance1EndingHour, 
		(time + myFlexibleAppliance1HoursNeeded - 1) % numberOfHoursInADay);

	boolean startingPointValid = isTimeWithinBounds(myFlexibleAppliance1StartingHour, myFlexibleAppliance1EndingHour, time);
	return startingPointValid && endingPointValid;
}

private boolean isAppliance2WithinBounds(int time) {
	boolean endingPointValid = isTimeWithinBounds(myFlexibleAppliance2StartingHour, 
		myFlexibleAppliance2EndingHour, 
		(time + myFlexibleAppliance2HoursNeeded - 1) % numberOfHoursInADay);

	boolean startingPointValid = isTimeWithinBounds(myFlexibleAppliance2StartingHour, myFlexibleAppliance2EndingHour, time);
	return startingPointValid && endingPointValid;
}

	// searches for available lower PAR. 
	// if a lower PAR is found, returns true
	// if it returns false, that means PAR has not decreased from last round to this round
private boolean exhaustiveSearch() {

	double tempPAR = 0.0;
	boolean isPARDecreased = false;


	for(int appliance1StartingHour = myFlexibleAppliance1StartingHour; 
		isAppliance1WithinBounds(appliance1StartingHour); 
		appliance1StartingHour = (appliance1StartingHour + 1) % numberOfHoursInADay ) {

		for(int appliance2StartingHour = myFlexibleAppliance2StartingHour;
			isAppliance2WithinBounds(appliance2StartingHour);
			appliance2StartingHour = (appliance2StartingHour + 1) % numberOfHoursInADay ) {
		
			removeMyFlexibleAppliancesFromConsumption();

		addMyFlexibleAppliancesToConsumptionGivenStartingPoint(appliance1StartingHour, appliance2StartingHour);
		myFlexibleAppliance1CorrectStartingHour = appliance1StartingHour;
		myFlexibleAppliance2CorrectStartingHour = myFlexibleAppliance2StartingHour;

		tempPAR = computePAR();
				if(PAR > tempPAR) { // we have found a new minimum
				
					PAR = tempPAR;
					
					isPARDecreased = true; 
					
					myFlexibleAppliance1LowestStartingHour = appliance1StartingHour;

					myFlexibleAppliance2LowestStartingHour = appliance2StartingHour;

					System.arraycopy(myConsumptionArray, 0, myLowestConsumptionArray, 0, myConsumptionArray.length);
					copyMyConsumptionFromArrayToString();
					minimumSystemPAR=PAR;
				}

			}
		}

		return isPARDecreased;
	}

	private void populateInflexibleAppliances() {

		double kWh = 0.0;
		double tempConsumption = 0.0; 
		int tempUsageBool = 0; // changes every hour, 1 if used, 0 if not
		StringBuffer tempConsumptionPerDay = new StringBuffer();


		// [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1] × 0.07 (kWh) Refrigerator
		kWh = 0.07;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1] × 0.05 (kWh) Lighting type 1
		kWh = 0.05;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [1 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 1 1 1 1 1 1 1] × 0.1 (kWh) Lighting type 2
		kWh = 0.1;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "1 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 1 1 1 1 1 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [1 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1] × 0.15 (kWh) Audio Visual Devices
		kWh = 0.15;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "1 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1] × 1.6 (kWh) Air conditioning
		kWh = 1.6;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0] × 1.5 (kWh) Cooking
		kWh = 1.5;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1] × 2.5 (kWh) Water Heating
		kWh = 2.5;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1] × 0.3 (kWh) Computers
		kWh = 0.3;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);

		// [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1] × 0.04 (kWh) Phone, Network (Modem) and standby devices
		kWh = 0.04;
		tempConsumption = 0.0;
		tempUsageBool = 0;

		for(String s : "1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1".split("\\s+") ) {
			tempUsageBool = Integer.parseInt(s);
			tempConsumption = tempUsageBool * kWh;
			tempConsumptionPerDay.append(String.valueOf(tempConsumption) + " ");
		}

		inflexibleAppliances.add(tempConsumptionPerDay.toString());
		tempConsumptionPerDay.setLength(0);
	}

	private void computePeak(String consumption0, String consumption1, String consumption2) {

		double tempPeak = 0.0;
		double temp = 0.0;

		String consumption0Split[] = consumption0.split("\\s+");
		String consumption1Split[] = consumption1.split("\\s+");
		String consumption2Split[] = consumption2.split("\\s+");

		for(int i = 0; i < numberOfHoursInADay; i++) {
			temp =  Double.parseDouble(consumption0Split[i]) + Double.parseDouble(consumption1Split[i]) + Double.parseDouble(consumption2Split[i]);
			if(temp > tempPeak) {
				tempPeak = temp;
				systemPeakHour = i;
			}
		}

		systemPeak = tempPeak;
	}

	private void computeAverage(String consumption0, String consumption1, String consumption2) {

		double temp = 0.0;

		String[] s1 = (consumption0.split("\\s+"));
		String[] s2 = (consumption1.split("\\s+"));
		String[] s3 = (consumption2.split("\\s+"));

		double[] totalConsumptionOfSystem = new double[numberOfHoursInADay];

		for(int i = 0; i < numberOfHoursInADay; i++) {
			totalConsumptionOfSystem[i] = Double.parseDouble(s1[i]);
			totalConsumptionOfSystem[i] += Double.parseDouble(s2[i]);
			totalConsumptionOfSystem[i] += Double.parseDouble(s3[i]);
		}

		for(int i = 0; i < numberOfHoursInADay; i++) {
			temp += totalConsumptionOfSystem[i];
		}

		systemAverage = temp / numberOfHoursInADay;

	}

	private void computeVariance() {

		computeAverage(consumption0, consumption1, myConsumption);

		String[] s1 = (consumption0.split("\\s+"));
		String[] s2 = (consumption1.split("\\s+"));
		String[] s3 = (myConsumption.split("\\s+"));

		double[] totalConsumptionOfSystem = new double[numberOfHoursInADay];

		for(int i = 0; i < numberOfHoursInADay; i++) {
			totalConsumptionOfSystem[i] = Double.parseDouble(s1[i]);
			totalConsumptionOfSystem[i] += Double.parseDouble(s2[i]);
			totalConsumptionOfSystem[i] += Double.parseDouble(s3[i]);
		}

		double varianceTemp = 0.0;

		for(int i = 0; i < numberOfHoursInADay; i++) {
			varianceTemp = varianceTemp + ( (totalConsumptionOfSystem[i] - systemAverage) * (totalConsumptionOfSystem[i] - systemAverage)  );
		}

		varianceTemp /= numberOfHoursInADay;

		systemVariance = varianceTemp;

	}

	private void printTotalSystemConsumption() {
		double[] totalConsumptionOfSystem = new double[numberOfHoursInADay];


		String[] s1 = (consumption0.split("\\s+"));
		String[] s2 = (consumption1.split("\\s+"));
		String[] s3 = (myConsumption.split("\\s+"));

		for(int i = 0; i < numberOfHoursInADay; i++) {
			totalConsumptionOfSystem[i] = Double.parseDouble(s1[i]);
			totalConsumptionOfSystem[i] += Double.parseDouble(s2[i]);
			totalConsumptionOfSystem[i] += Double.parseDouble(s3[i]);
		}
		for(int i = 0; i < numberOfHoursInADay; i++)
			System.out.println("Hour " + (i + 1) + " is " + totalConsumptionOfSystem[i]);
	}


  private void printThreeStrings() {
    System.out.println("1 - " + consumption0);
    System.out.println("2 - " + consumption1);
    System.out.println("me - " + myConsumption);
  }
}
