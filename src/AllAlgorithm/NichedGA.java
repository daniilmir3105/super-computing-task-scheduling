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

public class NichedGA {
	private static List<Cloudlet> cloudletList = new ArrayList<Cloudlet>(); ;
	private static List<Vm> vmList= new ArrayList<Vm>();
	private static int smallPopSize=10;
	public static void main(String[] args) {
		String testData="data\\cloudlets.txt";
		int taskNum=60;
		long startNichedGA=System.currentTimeMillis();
		String finishTmNichedGA=Runtest(testData,taskNum);
		long finishNichedGA=System.currentTimeMillis();
		long trainTmNichedGA=finishNichedGA-startNichedGA;
		System.out.println("This schedule plan takes "+finishTmNichedGA+" ms to finish execution by NicheGA,"+"spended "+trainTmNichedGA+"ms to train");
	}
	@SuppressWarnings("finally")
	public static String Runtest(String dataFilePath,int taskNum)
	{
		Log.printLine("Starting to run simulations...in GA");
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
			broker.submitCloudletList(cloudletList);
			//run simulation_pso
			boolean isGAscheduleApplied=true;
			if(isGAscheduleApplied)
			{
				finishTm=runSimulation_GA(broker);
			}
			else
			{
				runSimulation_RR(broker);
			}
//			Log.printLine("\nThe simulation is finished in GA!");
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}finally {
			return finishTm;
		}
	}
	public static void runSimulation_RR(DatacenterBroker broker)
	{
		CloudSim.startSimulation();
		// Final step: Print results when simulation is over
		List<Cloudlet> newList = broker.getCloudletReceivedList();

		CloudSim.stopSimulation();
		
		for(Vm vm:vmList)
		{
			Log.printLine(String.format("vm id= %s ,mips = %s ",vm.getId(),vm.getMips()));
		}
		String finishTm=printCloudletList(newList);
		System.out.println("This schedule plan takes "+finishTm+" ms to finish execution.");
	}
	
	public static String runSimulation_GA(DatacenterBroker broker)
	{
		//GA�����㷨�Ĳ���
		int popsize=200;
		int gmax=100;
		double crossoverProb=0.8;
		double mutationRate=0.01;
		//ִ��GA���ȷ���
		applyGAscheduling(popsize,gmax,crossoverProb,mutationRate);
		
		CloudSim.startSimulation();
		
		// Final step: Print results when simulation is over
		List<Cloudlet> newList = broker.getCloudletReceivedList();

		CloudSim.stopSimulation();
		
		for(Vm vm:vmList)
		{
			Log.printLine(String.format("vm id= %s ,mips = %s ",vm.getId(),vm.getMips()));
		}
		String finishTm=printCloudletList(newList);
//		System.out.println("This schedule plan takes "+finishTm+" ms to finish execution by NichedGA.");
		return finishTm;
		//����ÿ��ִ��GA�����㷨�ĵ��Ƚ������ͬ(����GA�����м����������,�������ܱ�RR����),����3�д�����ȡn�ε��ȷ���������GA�����ƽ��ִ��ʱ��.
//		int n=10;
//		double avgRuntime=getAvgRuntimeByGAscheduling(n,popsize,gmax,crossoverProb,mutationRate);
//		System.out.println(String.format("==============Printing the average runningtime GA schedule plans ===================\nAvg runtime of (n=%d) GA schedule plans:%.2f ms.",n,avgRuntime));
		
	}
	
	public static double getAvgRuntimeByGAscheduling(int times,int popSize,int gmax,double crossoverProb,double mutationRate)
	{
		double sum=0;
		for(int i=0;i<times;i++)
		{
			int[] schedule=getScheduleByGA( popSize, gmax, crossoverProb, mutationRate);
			sum+=getFitness(schedule);
		}
		return sum/times;
	}
	
	public static void applyGAscheduling(int popSize,int gmax,double crossoverProb,double mutationRate)
	{
		int[] schedule=getScheduleByGA(popSize,gmax,crossoverProb,mutationRate);
		assignResourcesWithSchedule(schedule);
	}
	
	public static void assignResourcesWithSchedule(int []schedule)
	{
		for(int i=0;i<schedule.length;i++)
		{
			getCloudletById(i).setVmId(schedule[i]);
		}
	}
	
	private static int[] findBestSchedule(ArrayList<int[]> pop)
	{
		double bestFitness=1000000000;
		int bestIndex=0;
		for(int i=0;i<pop.size();i++)
		{
			int []schedule=pop.get(i);
			double fitness=getFitness(schedule);
			if(bestFitness>fitness)
			{
				bestFitness=fitness;
				bestIndex=i;
			}
		}
		return pop.get(bestIndex);
	}
	private static double getBestFitness(ArrayList<int[]> pop)//��ȡһ����Ⱥ�������Ǹ���������
	{
		double bestFitness=1000000000;
		int bestIndex=0;
		for(int i=0;i<pop.size();i++)
		{
			int []schedule=pop.get(i);
			double fitness=getFitness(schedule);
			if(bestFitness>fitness)
			{
				bestFitness=fitness;
				bestIndex=i;
			}
		}
		return bestFitness;
	}
	
	private static int[] getScheduleByGA(int popSize,int gmax,double crossoverProb,double mutationRate)
	{
		int smallPopNumber=popSize/smallPopSize;
		ArrayList<int[]> pop=initPopsRandomly(cloudletList.size(),vmList.size(),popSize);//��˼����ÿ������ڼ������������ĸ�vm��Arraylist��Ϊ�����һ����Ⱥ�кܶ����
		ArrayList<int[]> smallPop=new ArrayList<int[]>();
		ArrayList<int[]> smallChildren=new ArrayList<int[]>();
		ArrayList<int[]> bestChildren=new ArrayList<int[]>();
		for(int j=0;j<smallPopNumber;j++) {
			for(int i=0;i<smallPopSize;i++) {
				smallPop.add(pop.get(i+smallPopSize*j));
			}
			smallChildren=GA(smallPop,gmax,crossoverProb,mutationRate);
			System.out.println("");
			smallPop.clear();
			bestChildren.add(findBestSchedule(smallChildren));
		}
//		pop=GA(pop,gmax,crossoverProb,mutationRate);
		return findBestSchedule(bestChildren);
	}
	
	private static ArrayList<int[]> initPopsRandomly(int taskNum,int vmNum,int popsize)
	{
		ArrayList<int[]> schedules=new ArrayList<int[]>();
		for(int i=0;i<popsize;i++)
		{
			//data structure for saving a schedule��array,index of array are cloudlet id,content of array are vm id.
			int[] schedule=new int[taskNum];
			for(int j=0;j<taskNum;j++)
			{
				schedule[j]=new Random().nextInt(vmNum);
			}
			schedules.add(schedule);
		}
		return schedules;
	}
	
	private static double getFitness(int[] schedule)//һ����������ʱ������,���ʱ��
	{
		double fitness=0;

		HashMap<Integer,ArrayList<Integer>> vmTasks=new HashMap<Integer,ArrayList<Integer>>();//ÿ�������������
		int size=cloudletList.size();
		
		for(int i=0;i<size;i++)
		{
			if(!vmTasks.keySet().contains(schedule[i]))
			{
				ArrayList<Integer> taskList=new ArrayList<Integer>();
				taskList.add(i);
				vmTasks.put(schedule[i],taskList);
			}
			else
			{
				vmTasks.get(schedule[i]).add(i);
			}
		}

		for(Entry<Integer, ArrayList<Integer>> vmtask:vmTasks.entrySet())//����hashmap,�����ִ��ʱ�䳤��vm
		{
			int length=0;
			for(Integer taskid:vmtask.getValue())
			{
				length+=getCloudletById(taskid).getCloudletLength();
			}
			
			double runtime=length/getVmById(vmtask.getKey()).getMips();
			if (fitness<runtime)
			{
				fitness=runtime;
			}
		}
		
		return fitness;
	}

	private static ArrayList<int[]> GA(ArrayList<int[]> pop,int gmax,double crossoverProb,double mutationRate)
	{
		HashMap<Integer,double[]> segmentForEach=calcSelectionProbs(pop);
		ArrayList<int[]> children=new ArrayList<int[]>();//Ҳ��һ����Ⱥ���к�parentsһ���Ĵ�С
		ArrayList<int[]> tempParents=new ArrayList<int[]>();
		while(children.size()<pop.size())
		{	
			//selection phase:select two parents each time.
			for(int i=0;i<2;i++)
			{
				double prob = new Random().nextDouble();
				for (int j = 0; j < pop.size(); j++)
				{
					if (isBetween(prob, segmentForEach.get(j)))
					{
						tempParents.add(pop.get(j));
						break;
					}
				}
			}
			//cross-over phase.
			int[] p1,p2,p1temp,p2temp;
			p1= tempParents.get(tempParents.size() - 2).clone();
			p1temp= tempParents.get(tempParents.size() - 2).clone();
			p2 = tempParents.get(tempParents.size() -1).clone();
			p2temp = tempParents.get(tempParents.size() -1).clone();
			if(new Random().nextDouble()<crossoverProb)
			{
				int crossPosition = new Random().nextInt(cloudletList.size() - 1);
				//cross-over operation
				for (int i = crossPosition + 1; i < cloudletList.size(); i++)
				{
					int temp = p1temp[i];
					p1temp[i] = p2temp[i];
					p2temp[i] = temp;
				}
			}
			//choose the children if they are better,else keep parents in next iteration.
			children.add(getFitness(p1temp) < getFitness(p1) ? p1temp : p1);
			children.add(getFitness(p2temp) < getFitness(p2) ? p2temp : p2);	
			// mutation phase.
			if (new Random().nextDouble() < mutationRate)
			{
				// mutation operations bellow.
				int maxIndex = children.size() - 1;

				for (int i = maxIndex - 1; i <= maxIndex; i++)
				{
					operateMutation(children.get(i));
				}
			}
		}
		System.out.print(getBestFitness(children)+" ");
		gmax--;//�Ŵ��Ĵ���
		return gmax > 0 ? GA(children, gmax, crossoverProb, mutationRate): children;
	}
	
	public static void operateMutation(int []child)
	{
		int mutationIndex = new Random().nextInt(cloudletList.size());
		int newVmId = new Random().nextInt(vmList.size());
		while (child[mutationIndex] == newVmId)
		{
			newVmId = new Random().nextInt(vmList.size());
		}

		child[mutationIndex] = newVmId;
	}
	
	private static boolean isBetween(double prob,double[]segment)
	{
		if(segment[0]<=prob&&prob<=segment[1])
			return true;
		return false;	
	}
	
	private static HashMap<Integer,double[]> calcSelectionProbs(ArrayList<int[]> parents)
	{
		int size=parents.size();
		double totalFitness=0;	
		ArrayList<Double> fits=new ArrayList<Double>();
		HashMap<Integer,Double> probs=new HashMap<Integer,Double>();
		
		for(int i=0;i<size;i++)
		{
			double fitness=getFitness(parents.get(i));
			fits.add(fitness);
			totalFitness+=fitness;
		}
		for(int i=0;i<size;i++)
		{
			probs.put(i,fits.get(i)/totalFitness );//ÿ�����ȵĿ���ֵ
		}
		
		return getSegments(probs);
	}
	
	private static HashMap<Integer,double[]> getSegments(HashMap<Integer,Double> probs)//ÿ�����彻��Ĵ�С
	{
		HashMap<Integer,double[]> probSegments=new HashMap<Integer,double[]>();
		//probSegments����ÿ�������ѡ����ʵ���㡢�յ㣬�Ա�ѡ����Ϊ����Ԫ�ء�
		int size=probs.size();
		double start=0;
		double end=0;
		for(int i=0;i<size;i++)
		{
			end=start+probs.get(i);
			double[]segment=new double[2];
			segment[0]=start;
			segment[1]=end;
			probSegments.put(i, segment);
			start=end;
		}
		
		return probSegments;
	}
	//����cloudlet����ӵ�cloudletList
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
	//������������
	private static Datacenter createDatacenter(String name)
	{
		List<Host> hostList = new ArrayList<Host>();//����
		List<Pe> peList = new ArrayList<Pe>();//�����cpu
		
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
		//�����ɵ���������ʱ�̾��ǵ��ȷ�������ִ��ʱ��
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
