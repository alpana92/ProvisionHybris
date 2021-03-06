package com.hybris.environment;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;

import com.hybris.HybrisRecipe;
import com.hybris.HybrisVersion;
import com.hybris.ansible.Ansible;
import com.hybris.provider.Provider;


public class Environment {
	
	private String projectCode;
	private EnvironmentType environmentType;
	private Provider provider;
	private static final String REPO_SERVER_IP = "54.152.16.177";
	private static final String DOMAIN=".hybrishosting.com";
	
	public Environment(Provider provider, String projectCode, EnvironmentType environmentType) {
		// TODO Auto-generated constructor stub
		this.setProvider(provider);
		this.setProjectCode(projectCode);
		this.setEnvironmentType(environmentType);
	}

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public String getProjectCode() {
		return projectCode;
	}

	public void setProjectCode(String project_code) {
		if(Character.isDigit(project_code.charAt(0))){
			System.out.println("Project code should start from a letter.");
			this.projectCode=null;
			System.exit(0);
		}else if(project_code.matches("^[a-zA-Z0-9]*$")){
			this.projectCode = project_code.toLowerCase();
		}else if(project_code.length() != 9){
			System.out.println("Project code length should be 9");
			this.projectCode=null;
			System.exit(0);
		}else{
			System.out.println("Special charachters are not accepteble for project code.");
			this.projectCode=null;
			System.exit(0);
		}
	}

	public EnvironmentType getEnvironmentType() {
		return environmentType;
	}

	public void setEnvironmentType(EnvironmentType environment_type) {
		this.environmentType = environment_type;
	}
	
	public String getHostName(Server server){
		String hostname = "";
		ServerType serverType = server.getServerType();
		hostname = this.projectCode + "-" + this.environmentType.getCode() + "-" + this.provider.getCode() + "-" 
				+ serverType.getCode() + "-001";
		return hostname;
	}
	
	public String getSiteName(String projectCode, EnvironmentType environmentType){
		return this.getProvider().getCode() + "." + projectCode + ".alpana.info";
	}
	
	public HashMap<ServerType, ServerInstance> create(ComputeService computeService, EnvironmentKind environmentKind, 
			                                          HybrisRecipe hybrisRecipe, HybrisVersion hybrisVersion){
		
		HashMap<ServerType, ServerInstance> environmentMap = new HashMap<ServerType, ServerInstance>();
		Server servers[] = environmentKind.getServers();
		
		if(servers.length == 0){
			System.out.println("Server list is empty!");
			return environmentMap;
		}
		
		long timeStart = System.currentTimeMillis();
		System.out.println(">> Creating server instances ..");
		
		try {
			
			for(Server server:servers){
				Template template = server.getTemplate(computeService, this.getProvider(), this.getEnvironmentType());
				String hostname = this.getHostName(server);
				ServerInstance serverInstance = server.create(computeService, template, hostname);
				environmentMap.put(server.getServerType(), serverInstance);
			}
			
			System.out.println("<< Server Instances are created for " + this.projectCode + "-" + this.getEnvironmentType().getCode());
			
			long provisionTimeEnd = System.currentTimeMillis();
			long provisionDuration = provisionTimeEnd - timeStart;
			long provisionMinutes = TimeUnit.MILLISECONDS.toMinutes(provisionDuration);
			System.out.println("Time utilised for execution of Provisioning part: " + provisionMinutes + " minutes");
			
			System.out.println(">> Creating ansible inventory file");
			System.out.println("");
			
			Ansible ansible = new Ansible();
			String inventory = ansible.getInventoryFile(this.projectCode, this.environmentType);
			String inventoryLog = ansible.getInventoryLogFile(this.projectCode, this.environmentType);
			String groupVars = ansible.getGroupVarsFile(this.projectCode, this.environmentType);
			String siteName = this.getSiteName(this.projectCode, this.environmentType);
			String projectGroup = this.projectCode + "_" + this.environmentType.getCode();
			
			ansible.executeCommand("echo \"---\" >>" + groupVars + "; "
									+ "echo \"repo_server: " + REPO_SERVER_IP + "\" >>" + groupVars + "; "
									+ "echo \"java_version: " + hybrisVersion.getJavaVersion().getVersion() + "\" >>" + groupVars + "; "
									+ "echo \"java_package: " + hybrisVersion.getJavaVersion().getPackageName() + "\" >>" + groupVars + "; "
									+ "echo \"hybris_version: " + hybrisVersion.getVersion() + "\" >>" + groupVars + "; "
									+ "echo \"hybris_package: " + hybrisVersion.getPackageName() + "\" >>" + groupVars + "; "
									+ "echo \"hybris_recipe: " + hybrisRecipe.getRecipeId() + "\" >>" + groupVars + "; "
									+ "echo \"solr_package: " + hybrisVersion.getSolrPackage() + "\" >>" + groupVars + "; "
									+ "echo \"db_driver: mysql-connector-java-5.1.33-bin.jar\" >>" + groupVars + "; "
									+ "echo \"site_name: " + siteName + "\" >>" + groupVars + "; "
									+ "echo \"domain_name: " + DOMAIN + "\" >>" + groupVars + "; "
									+ "echo \"default_shop: " + hybrisRecipe.getDefaultShop() + "\" >>" + groupVars + "; "
									+ "echo \"servers_list:\" >>" + groupVars);
			
			ansible.executeCommand("echo \"[all:vars]\" >>" + inventory + "; "
					               + "echo \"ansible_python_interpreter=/usr/bin/python3\" >>" + inventory + "; "
					               + "echo \"ansible_ssh_common_args='-o StrictHostKeyChecking=no'\n\" >>" + inventory + "; "
					               + "echo \"[" + projectGroup + ":children]\" >>" + inventory);
			
		   for(Server server:servers){
			   ansible.executeCommand("echo \"" + server.getServerType().getCode() + "\" >>" + inventory + "; ");
		   }
		   
		   String siteIP = "";
		   for(Server server:servers){
			   ServerInstance instance = environmentMap.get(server.getServerType());
			   String instanceIp = computeService.getNodeMetadata(instance.getNodeId()).getPublicAddresses().iterator().next();
			   if(server.getServerType().equals(ServerType.Web)){
				   siteIP = instanceIp;
			   }
			   ansible.executeCommand("echo \"\n[" + server.getServerType().getCode() + ":children]\" >>" + inventory + "; "
					                  + "echo \"" + instance.getHostname() + "\n\" >>" + inventory + "; "
					                  + "echo \"[" + instance.getHostname() + "]\" >>" + inventory + "; "
					                  + "echo \"" + instanceIp + "\" >>" + inventory + "; ");
			   
			   ansible.executeCommand("echo \" - type: " + server.getServerType().getCode() + "\" >>" + groupVars + "; "
					                  + "echo \"   name: " + instance.getHostname() + "\" >>" + groupVars + "; "
					                  + "echo \"   ip: " + instanceIp + "\" >>" + groupVars + "; ");
		   }
		   
		   System.out.println("<< Inventory file is created on Ansible server at " + inventory);
		   System.out.println("");
		   System.out.println(">> Creating Environment ...");
		   System.out.println("");
		   
		   System.out.println(">> Creating inventory log file on Ansible server at " + inventoryLog);
		   computeService.getContext().close();
		   System.out.println("");
		   ansible.executeCommand("ansible-playbook " + Ansible.CREATE_ENVIRONMENT_PLAYBOOK + " -i " + inventory
				                  + " >> " + inventoryLog);
		   
		   ansible.getComputeService().getContext().close();
		   
		   System.out.println("Shop is available on " + siteName + " with IP: " + siteIP);
		   
		   long deploymentTimeEnd = System.currentTimeMillis();
		   long deploymentDuration = deploymentTimeEnd - provisionTimeEnd;
		   long deploymentMinutes = TimeUnit.MILLISECONDS.toMinutes(deploymentDuration);
		   System.out.println("Time utilised for execution of Deployment part: " + deploymentMinutes + " minutes");
		   
		   long totalDuration = deploymentTimeEnd - timeStart;
		   long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalDuration);
		   System.out.println("Time utilised for execution of Deployment part: " + totalMinutes + " minutes");
		   
		} catch (Exception e) {
			// TODO: handle exception
		}
		return environmentMap;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args){
		
		try{
			
			Provider provider = Provider.AmazonWebService;
			ComputeService computeService = provider.getComputeService();
			String projectCode="demo";
			Environment environment = new Environment(provider, projectCode, 
													   EnvironmentType.Development);
			environment.create(computeService, EnvironmentKind.NonClustered, 
											   HybrisRecipe.B2B_Accelerator, 
											   HybrisVersion.Hybris6_3_0);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
}