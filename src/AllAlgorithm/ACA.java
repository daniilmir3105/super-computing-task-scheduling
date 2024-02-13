package AllAlgorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class ACA {
	private static List<Cloudlet> cloudletList = new ArrayList<Cloudlet>(); ;
	private static List<Vm> vmList= new ArrayList<Vm>();;

	private static double p=0.5;//Ã¿´Îµü´úºó£¬ÐÅÏ¢ËØË¥¼õµÄ±ÈÀý
	private static double q=2;//Ã¿´Îµü´úºó£¬ÐÅÏ¢ËØÔö¼ÓµÄ±ÈÀý
	private static int iteratorNum=100;//µü´úµÄÊýÁ¿
	private static int antNum=20;//Ã¿´Îµü´úÖÐÂìÒÏµÄÊýÁ¿
	private static HashMap<Integer,ArrayList<Double>> resultData=new HashMap<Integer,ArrayList<Double>>();
	
	@SuppressWarnings("finally")
	public static String Runtest(String dataFilePath,int taskNum)
	{
		Log.printLine("Starting to run simulations...in ACA");
		String finishTm="";
		try
		{
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
		
			CloudSim.init(num_user, calendar, trace_flag);

			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			// #3 step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();
			// #4 step: Create 5 virtual machines
			// VM description
			long size = 10000; // image size (MB)
			int ram = 512; // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of cpus
			String vmm = "Xen"; // VMM name

			Vm vm1 = new Vm(0, brokerId, 5000, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerSpaceShared());
			Vm vm2 = new Vm(1, brokerId, 2500, pesNumber, ram, bw, size,
					vmm,new CloudletSchedulerTimeShared());
			Vm vm3 = new Vm(2, brokerId, 2500, pesNumber, ram, bw, size,
					vmm,new CloudletSchedulerTimeShared());
			Vm vm4 = new Vm(3, brokerId, 1500, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerSpaceShared());
			Vm vm5 = new Vm(4, brokerId, 1000, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerSpaceShared());

			// add the VMs to the vmList
			vmList.add(vm1);
			vmList.add(vm2);
			vmList.add(vm3);
			vmList.add(vm4);
			vmList.add(vm5);

			// submit vm list to the broker
			broker.submitVmList(vmList);
			//create cloudlets and submit them.
			createTasks(brokerId,dataFilePath,taskNum);
//			ACA();
			broker.submitCloudletList(cloudletList);
			finishTm=runSimulation_ACA(broker);
//			boolean isGAscheduleApplied=true;
//			if(isGAscheduleApplied)
//			{
//				runSimulation_GA(broker);
//			}
//			else
//			{
//				runSimulation_RR(broker);
//			}
			Log.printLine("\nThe simulation is finished in ACA!");
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}finally{
			return finishTm;
		}
	}
	private static String runSimulation_ACA(DatacenterBroker broker) {
		// TODO Auto-generated method stub
		applyACAscheduling();
		CloudSim.startSimulation();
		List<Cloudlet> newList=broker.getCloudletReceivedList();
		CloudSim.stopSimulation();
		
//		for(Vm vm:vmList) {
//			Log.printLine(String.format("vm id=s%,mips=s%",vm.getId(),vm.getMips()));
//		}
		
		String finishTm=printCloudletList(newList);
		for(int i=0;i<iteratorNum-1;i++) {
			int minIndex=getMinAntIndex(resultData.get(i));
			System.out.print(resultData.get(i).get(minIndex)+" ");
		}
//		System.out.println("This schedule plan takes "+finishTm+" ms to finish execution.");
		return finishTm;
	}
	private static void applyACAscheduling() {
		// TODO Auto-generated method stub
		int[] schedule=ACA();
		assignResourcesWithSchedule(schedule);
	}
	//ÒÔÏÂÊÇaca
	private static int[] ACA() {
		// TODO Auto-generated method stub
		double[][] timeMatrix=new double[cloudletList.size()][vmList.size()];
		double[][] pheromoneMatrix=new double[cloudletList.size()][vmList.size()];
		int[] maxPheromoneMatrix=new int[cloudletList.size()];//·ÖÅäµÄÈÎÎñiµÄÐÅÏ¢ËØ×î¶àµÄÐÅÏ¢ËØ¾ØÕóÏÂ±ê
		long[] criticalPointMatrix=new long[cloudletList.size()];//½«0µ½5µÄÂìÒÏµÄÈÎÎñ¶¼·ÖÅä¸øÐÅÏ¢ËØÅ¨¶È×î¸ßµÄ½Úµã´¦ÀíÆäËûËæ»ú·ÖÅä
		initTimeMatrix(cloudletList,vmList,timeMatrix);//³õÊ¼»¯ÈÎÎñÖ´ÐÐÊ±¼ä¾ØÕó
		initPheromoneMatrix(cloudletList,vmList,pheromoneMatrix,maxPheromoneMatrix,criticalPointMatrix);//³õÊ¼»¯ÐÅÏ¢ËØ¾ØÕó
//		DecimalFormat dft = new DecimalFormat("###.##");
//		double finishTm1=resultData.get(90).get(0);
//		System.out.println("This schedule plan takes "+dft.format(finishTm1)+" ms to finish execution by ACA");
		return acaSearch(iteratorNum,antNum,timeMatrix,pheromoneMatrix,maxPheromoneMatrix,criticalPointMatrix);//µü´úËÑË÷,·µ»Øµ÷¶ÈÊý×é
	}
	//³õÊ¼»¯ÈÎÎñÖ´ÐÐÊ±¼ä¾ØÕó
	public static void initTimeMatrix(List<Cloudlet> cloudletList,List<Vm> vmList,double[][] timeMatrix) {
	    for (int i=0; i<cloudletList.size(); i++) {
	        // ·Ö±ð¼ÆËãÈÎÎñi·ÖÅä¸øËùÓÐ½ÚµãµÄ´¦ÀíÊ±¼ä
	        for (int j=0; j<vmList.size(); j++) {
	            timeMatrix[i][j]=(cloudletList.get(i).getCloudletLength()/vmList.get(j).getMips());
	        }
	    }
	}
	//³õÊ¼»¯ÐÅÏ¢ËØ¾ØÕó
	public static void initPheromoneMatrix(List<Cloudlet> cloudletList,List<Vm> vmList,double[][] pheromoneMatrix,int[] maxPheromoneMatrix,long[] criticalPointMatrix) {
	    for (int i=0; i<cloudletList.size(); i++) {
	        // ·Ö±ð¼ÆËãÈÎÎñi·ÖÅä¸øËùÓÐ½ÚµãµÄ´¦ÀíÊ±¼ä
	        for (int j=0; j<vmList.size(); j++) {
	            pheromoneMatrix[i][j]=1;
	        }
	        maxPheromoneMatrix[i]=0;
	        criticalPointMatrix[i]=0;
	    }
	}
	//µü´úËÑË÷È»ºó·µ»Øµ÷¶ÈÊý×é
	public static int[] acaSearch(int iteratorNum,int antNum,double[][] timeMatrix,double[][] pheromoneMatrix,int[] maxPheromoneMatrix,long[] criticalPointMatrix){
		int itCount;
        // ±¾´Îµü´úÖÐ£¬ËùÓÐÂìÒÏµÄÂ·¾¶
        HashMap<Integer,ArrayList<int[]>> pathMatrix_allAnt = new HashMap<Integer,ArrayList<int[]>>();
	    for (itCount=0; itCount<iteratorNum; itCount++) {
	        for (int antCount=0; antCount<antNum; antCount++) {
	            // µÚantCountÖ»ÂìÒÏµÄ·ÖÅä²ßÂÔ(pathMatrix[i][j]±íÊ¾µÚantCountÖ»ÂìÒÏ½«iÈÎÎñ·ÖÅä¸øj½Úµã´¦Àí)
	            ArrayList<int[]> pathMatrix_oneAnt = initMatrix(cloudletList.size(), vmList.size(), 0);
	            for (int taskCount=0; taskCount<cloudletList.size(); taskCount++) {
	                // ½«µÚtaskCount¸öÈÎÎñ·ÖÅä¸øµÚvmCount¸övm´¦Àí£¬¸ù¾ÝÐÅÏ¢ËØÅ¨¶È
	                int vmCount = assignOneTask(antCount, taskCount,maxPheromoneMatrix,criticalPointMatrix);
	                pathMatrix_oneAnt.get(taskCount)[vmCount] = 1;
	            }
	            // ½«µ±Ç°ÂìÒÏµÄÂ·¾¶¼ÓÈëpathMatrix_allAnt
	            pathMatrix_allAnt.put(antCount,pathMatrix_oneAnt);
	        }
	        // ¼ÆËã ±¾´Îµü´úÖÐ ËùÓÐÂìÒÏµÄÈÎÎñ´¦ÀíÊ±¼ä
	        ArrayList<Double> timeArray_oneIt = calTime_oneIt(pathMatrix_allAnt,timeMatrix);
	        // ½«±¾µØµü´úÖÐ ËùÓÐÂìÒÏµÄ ÈÎÎñ´¦ÀíÊ±¼ä¼ÓÈë×Ü½á¹û¼¯
	        resultData.put(itCount,timeArray_oneIt);

	        // ¸üÐÂÐÅÏ¢ËØ
	        updatePheromoneMatrix(pathMatrix_allAnt, pheromoneMatrix, timeArray_oneIt,maxPheromoneMatrix,criticalPointMatrix);
	    }
		int minIndex=getMinAntIndex(resultData.get(itCount-1));
		return getSchedule(pathMatrix_allAnt,minIndex);
	}
	//¸üÐÂÐÅÏ¢ËØ
	private static void updatePheromoneMatrix(HashMap<Integer, ArrayList<int[]>> pathMatrix_allAnt,
			double[][] pheromoneMatrix, ArrayList<Double> timeArray_oneIt,int[] maxPheromoneMatrix,long[] criticalPointMatrix) {
		// TODO Auto-generated method stub
	    // ËùÓÐÐÅÏ¢ËØ¾ùË¥¼õp%
	    for (int i=0; i<cloudletList.size(); i++) {
	        for (int j=0; j<vmList.size(); j++) {
	            pheromoneMatrix[i][j] *= p;
	        }
	    }

	    // ÕÒ³öÈÎÎñ´¦ÀíÊ±¼ä×î¶ÌµÄÂìÒÏ±àºÅ
	    double minTime = Double.MAX_VALUE;
	    int minIndex = -1;
	    for (int antIndex=0; antIndex<antNum; antIndex++) {
	        if (timeArray_oneIt.get(antIndex) < minTime) {
	            minTime = timeArray_oneIt.get(antIndex);
	            minIndex = antIndex;
	        }
	    }

	    // ½«±¾´Îµü´úÖÐ×îÓÅÂ·¾¶µÄÐÅÏ¢ËØÔö¼Óq%
	    for (int taskIndex=0; taskIndex<cloudletList.size(); taskIndex++) {
	        for (int vmIndex=0; vmIndex<vmList.size(); vmIndex++) {
	            if (pathMatrix_allAnt.get(minIndex).get(taskIndex)[vmIndex] == 1) {
	                pheromoneMatrix[taskIndex][vmIndex] *= q;
	            }
	        }
	    }


	    //¿ªÊ¼¼ÆËãmaxPheromoneMatrix,criticalPointMatrix;
	    for (int taskIndex=0; taskIndex<cloudletList.size(); taskIndex++) {
	        double maxPheromone = pheromoneMatrix[taskIndex][0];
	        int maxIndex = 0;
	        double sumPheromone = pheromoneMatrix[taskIndex][0];
	        boolean isAllSame = true;

	        for (int nodeIndex=1; nodeIndex<vmList.size(); nodeIndex++) {
	            if (pheromoneMatrix[taskIndex][nodeIndex] > maxPheromone) {
	                maxPheromone = pheromoneMatrix[taskIndex][nodeIndex];
	                maxIndex = nodeIndex;
	            }
	            if (pheromoneMatrix[taskIndex][nodeIndex] != pheromoneMatrix[taskIndex][nodeIndex-1]){
	                isAllSame = false;
	            }

	            sumPheromone += pheromoneMatrix[taskIndex][nodeIndex];
	        }

	        // Èô±¾ÐÐÐÅÏ¢ËØÈ«¶¼ÏàµÈ£¬ÔòËæ»úÑ¡ÔñÒ»¸ö×÷Îª×î´óÐÅÏ¢ËØ
	        if (isAllSame==true) {
	            maxIndex = new Random().nextInt(vmList.size() - 1);
	            maxPheromone = pheromoneMatrix[taskIndex][maxIndex];
	        }

	        // ½«±¾ÐÐ×î´óÐÅÏ¢ËØµÄÏÂ±ê¼ÓÈëmaxPheromoneMatrix
	        maxPheromoneMatrix[taskIndex]=maxIndex;

	        // ½«±¾´Îµü´úµÄÂìÒÏÁÙ½ç±àºÅ¼ÓÈëcriticalPointMatrix(¸ÃÁÙ½çµãÖ®Ç°µÄÂìÒÏµÄÈÎÎñ·ÖÅä¸ù¾Ý×î´óÐÅÏ¢ËØÔ­Ôò£¬¶ø¸ÃÁÙ½çµãÖ®ºóµÄÂìÒÏ²ÉÓÃËæ»ú·ÖÅä²ßÂÔ)
	        criticalPointMatrix[taskIndex]=Math.round(antNum * (maxPheromone/sumPheromone));
	    }
		
	}
	//³õÊ¼»¯Ò»Ö»ÂìÒÏµÄÂ·¾¶
	public static ArrayList<int[]> initMatrix(int taskNum,int vmNum,int defaultNum){
		ArrayList<int[]> matrix=new ArrayList<int[]>();
	    for (int i=0; i<cloudletList.size(); i++) {
	        // ·Ö±ð¼ÆËãÈÎÎñi·ÖÅä¸øËùÓÐ½ÚµãµÄ´¦ÀíÊ±¼ä
	    	int[] matrixOne=new int[vmList.size()];
	        for (int j=0; j<vmList.size(); j++) {
	            matrixOne[j]=defaultNum;
	        }
	        matrix.add(matrixOne);
	    }
	    return matrix;
	}
	//½«µÚtaskCount¸öÈÎÎñ·ÖÅä¸øÄ³Ò»¸övm´¦Àí
	public static int assignOneTask(int antCount, int taskCount,int[] maxPheromoneMatrix,long[] criticalPointMatrix) {
		if(antCount<=criticalPointMatrix[taskCount]) {
			return maxPheromoneMatrix[taskCount];
		}
		return new Random().nextInt(vmList.size() - 1);
	}
	//¼ÆËãÒ»´Îµü´úÖÐ£¬ËùÓÐÂìÒÏµÄÈÎÎñ´¦ÀíÊ±¼ä
	public static ArrayList<Double> calTime_oneIt(HashMap<Integer,ArrayList<int[]>> pathMatrix_allAnt,double[][] timeMatrix){
	    ArrayList<Double> time_allAnt = new ArrayList<Double>();
	    for (int antIndex=0; antIndex<pathMatrix_allAnt.size(); antIndex++) {
	        // »ñÈ¡µÚantIndexÖ»ÂìÒÏµÄÐÐ×ßÂ·¾¶
	        ArrayList<int[]> pathMatrix = pathMatrix_allAnt.get(antIndex);

	        // »ñÈ¡´¦ÀíÊ±¼ä×î³¤µÄvm¶ÔÓ¦µÄ´¦ÀíÊ±¼ä
	        double maxTime = -1;
	        for (int nodeIndex=0; nodeIndex<vmList.size(); nodeIndex++) {
	            // ¼ÆËã½ÚµãtaskIndexµÄÈÎÎñ´¦ÀíÊ±¼ä
	            double time = 0;
	            for (int taskIndex=0; taskIndex<cloudletList.size(); taskIndex++) {
	                if (pathMatrix.get(taskIndex)[nodeIndex] == 1) {
	                    time += timeMatrix[taskIndex][nodeIndex];
	                }
	            }
	            // ¸üÐÂmaxTime
	            if (time > maxTime) {
	                maxTime = time;
	            }
	        }
	        time_allAnt.add(maxTime);
	    }
	    return time_allAnt;
	}
	//»ñÈ¡×î¶ÌÊ±¼äµÄÂìÒÏ±àºÅÒÔ¼°Ê±¼ä
	public static int getMinAntIndex(ArrayList<Double> timeArray_oneIt) {
	    // ÕÒ³öÈÎÎñ´¦ÀíÊ±¼ä×î¶ÌµÄÂìÒÏ±àºÅ
	    double minTime = Double.MAX_VALUE;
	    int minIndex = -1;
	    for (int antIndex=0; antIndex<antNum; antIndex++) {
	        if (timeArray_oneIt.get(antIndex) < minTime) {
	            minIndex = antIndex;
	            minTime=timeArray_oneIt.get(antIndex);
	        }
	    }
	    return minIndex;
	}
	//»ñÈ¡×îÓÅµ÷¶ÈÊý×é
	public static int[] getSchedule(HashMap<Integer, ArrayList<int[]>> pathMatrix_allAnt,int minIndex) {
		int[] schedule=new int[cloudletList.size()];
	    for (int taskIndex=0; taskIndex<cloudletList.size(); taskIndex++) {
	        for (int vmIndex=0; vmIndex<vmList.size(); vmIndex++) {
	            if (pathMatrix_allAnt.get(minIndex).get(taskIndex)[vmIndex] == 1) {
	                schedule[taskIndex]=vmIndex;
	            }
	        }
	    }
	    return schedule;
	}
	//aca½áÊø
	//·ÖÅäÈÎÎñµ½vm
	public static void assignResourcesWithSchedule(int []schedule)
	{
		for(int i=0;i<schedule.length;i++)
		{
			getCloudletById(i).setVmId(schedule[i]);
		}
	}
	//´´½¨cloudlet£¬Ìí¼Óµ½cloudletList
	private static void createTasks(int brokerId,String filePath, int taskNum)
	{
		try
		{
			@SuppressWarnings("resource")
			BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
			String data = null;
			int index = 0;
			
			//cloudlet properties.
			int pesNumber = 1;
			long fileSize = 1000;
			long outputSize = 1000;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			while ((data = br.readLine()) != null)
			{
				System.out.println(data);
				String[] taskLength=data.split("\t");
				for(int j=0;j<20;j++){
					Cloudlet task=new Cloudlet(index+j, (long) Double.parseDouble(taskLength[j]), pesNumber, fileSize,
							outputSize, utilizationModel, utilizationModel,
							utilizationModel);
					task.setUserId(brokerId);
					cloudletList.add(task);
					if(cloudletList.size()==taskNum)
					{	
						br.close();
						return;
					}
				}
				//20 cloudlets each line in the file cloudlets.txt.
				index+=20;
			}
			
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	//´´½¨Êý¾ÝÖÐÐÄ
	private static Datacenter createDatacenter(String name)
	{
		List<Host> hostList = new ArrayList<Host>();//Ö÷»ú
		List<Pe> peList = new ArrayList<Pe>();//¼ÆËã»úcpu
		
		int mips = 5000;
		peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store MIPS Rating
		
		mips = 2500;
		peList.add(new Pe(1, new PeProvisionerSimple(mips))); 
		
		mips = 2500;
		peList.add(new Pe(2, new PeProvisionerSimple(mips))); 
		
		mips = 1500;
		peList.add(new Pe(3, new PeProvisionerSimple(mips)));
			
		mips = 1000;
		peList.add(new Pe(4, new PeProvisionerSimple(mips))); 
													
		int hostId = 0;
		int ram = 4096; // host memory (MB)
		long storage = 10000000; // host storage
		int bw = 10000;

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw), storage, peList,
				new VmSchedulerTimeShared(peList)));
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.001; // the cost of using bw in this resource
		
		//we are not adding SAN devices by now
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try
		{
			datacenter = new Datacenter(name, characteristics,
					new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return datacenter;
	}

	private static DatacenterBroker createBroker()
	{

		DatacenterBroker broker = null;
		try
		{
			broker = new DatacenterBroker("Broker");
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	private static String printCloudletList(List<Cloudlet> list)
	{
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("================ Execution Result ==================");
		Log.printLine("No."+indent +"Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent+"VM mips"+ indent +"CloudletLength"+indent+ "Time"
				+ indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++)
		{
			cloudlet = list.get(i);
			Log.print(i+1+indent+indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getStatus()== Cloudlet.SUCCESS)
			{
				Log.print("SUCCESS");

				Log.printLine(indent +indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getVmId()
						+ indent + indent + getVmById(cloudlet.getVmId()).getMips()
						+ indent + indent + cloudlet.getCloudletLength()
						+ indent + indent+ indent + indent
						+ dft.format(cloudlet.getActualCPUTime()) + indent
						+ indent + dft.format(cloudlet.getExecStartTime())
						+ indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}
		Log.printLine("================ Execution Result Ends here ==================");
		//×îºóÍê³ÉµÄÈÎÎñµÄÍê³ÉÊ±¿Ì¾ÍÊÇµ÷¶È·½°¸µÄ×ÜÖ´ÐÐÊ±¼ä
		return dft.format(list.get(size-1).getFinishTime());
	}

	public static Vm getVmById(int vmId)
	{
		for(Vm v:vmList)
		{
			if(v.getId()==vmId)
				return v;
		}
		return null;
	}
	
	public static Cloudlet getCloudletById(int id)
	{
		for(Cloudlet c:cloudletList)
		{
			if(c.getCloudletId()==id)
				return c;
		}
		return null;
	}
	
	public static void writeTxtAppend(String file, String conent)
	{
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(conent + "\r\n");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
