package com.hybris.provision;

import java.io.IOException;
import java.util.Set;

import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.domain.Location;

import com.hybris.service.CloudService;
import com.hybris.provider.Provider;
import com.hybris.provider.specifications.*;

/**
 * Main Provision App
 *
 */
public class ProvisionApp {

	public static void main( String[] args ) throws IOException{
		
		
		CloudService service = new CloudService(Provider.GoogleCloudProvider);
		
		
		/* ******************************************
		 *		GCP Create Node						*
		 * ******************************************/
		service.createNode(OsFamily.UBUNTU, Cpu.Two64bit, RamSize.Eight, DiskSize.Ten, 
							Region.GCP_UsEast1b, "hybris-dev-linux-007", "alpanachaphalkar", "C:\\cygwin64\\home\\D066624\\.ssh\\alpanachaphalkar.pub");
		
		
		
		/* ******************************************
		 *		Listing machine types in GCP		*
		 * ******************************************/			
/*		Set<? extends Hardware> machineTypes = service.initComputeService().listHardwareProfiles();
		System.out.println("<< GCP Hardware Profiles List: ");
		
		for(Hardware machineType:machineTypes){
			
			if(machineType.getLocation().getId().equals("us-east1-b")){
				System.out.println("Name: " + machineType.getName() + "  Location: " + machineType.getLocation().getId() + "  Ram: " + machineType.getRam() 
							   + "  Type: " + machineType.getType() + "  Processor: " + machineType.getProcessors() + "  Volumes: " 
					           + machineType.getVolumes() + "  User data: " + machineType.getUserMetadata());
			}
		}*/
		
		
		/* ******************************************
		 *		Listing locations in GCP			*
		 * ******************************************/		
/* 		Set<? extends Location> locations = service.initComputeService().listAssignableLocations();
		System.out.println("<< GCP Locations List: ");
		for(Location location:locations){
			System.out.println("Name: " + location.getDescription() + "  ID: " + location.getId());
		}*/

		
		
		
		/* ******************************************
		 *		GCP initiate compute service		*
		 * ******************************************/		
/*		service.initComputeService();
		System.out.println("Initialization successful!");*/
		
		
		
		
		/* ******************************************
		 *		AWS EC2 Create Node					*
		 * ******************************************/		
/* 		CloudService service = new CloudService(Provider.AmazonWebService);
		service.createNode(OsFamily.UBUNTU, Cpu.Two64bit, RamSize.Eight, DiskSize.Ten, 
							Region.AWS_UsEast1, "hybris-dev-linux-003", "trial3", "C:\\cygwin64\\home\\D066624\\.ssh\\trail2.pub");*/

		
		
		
		/* ******************************************
		 *		Listing locations in AWS			*
		 * ******************************************/
/*		Set<? extends Location> locations = service.initComputeService().listAssignableLocations();
		System.out.println("<< AWS Locations List: ");
		for(Location location:locations){
			System.out.println("Name: " + location.getDescription() + "  ID: " + location.getId());
		}*/
		
		
    }
}
