/*
Copyright 2020 The Kubernetes Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.kubernetes.client.examples;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JobExample {

	public static void main(String[] args) throws Exception {

    // The following codes demonstrates using generic client to manipulate jobs
    ApiClient apiClient = ClientBuilder.standard().build();
    GenericKubernetesApi<V1Job, V1JobList> jobClient =
            new GenericKubernetesApi<>(V1Job.class, V1JobList.class, "batch", "v1", "jobs", apiClient);
    
    // This job computes π to 2000 places and prints it out. It takes around 10s to complete.
    List<String> cmd = new ArrayList<String>( 
            Arrays.asList("perl",  "-Mbignum=bpi", "-wle", "print bpi(2000)"));  
    V1PodSpec podSpec = new V1PodSpec().containers(Arrays.asList(new V1Container().name("pi")
    		.image("perl").command(cmd))).restartPolicy("Never");
    V1Job piJob =
            new V1Job().kind("Job").metadata(new V1ObjectMeta().namespace("default").name("pi"))
            	.spec(new V1JobSpec()
                        .template(new V1PodTemplateSpec().spec(podSpec)));
    
    // Create the job
    V1Job jobCreated =
            jobClient
                .create(piJob)
                .onFailure(
                    errorStatus -> {
                      System.out.println("Not Created!");
                      throw new RuntimeException(errorStatus.toString());
                    })
                .getObject();
    
    System.out.println("Created a job:" + jobCreated.toString());
    
    // Check if the job succeeded.
    // A better way is to "watch" pods. See WatchExample.java
    while(true) {
    	Thread.sleep(2 * 1000);
    	KubernetesApiResponse<V1Job> jobResp = jobClient.get(piJob.getMetadata().getNamespace(), piJob.getMetadata().getName());
    	if(!jobResp.isSuccess()) {
    		System.out.println("Failed to get job from apiserver, continue");
    		continue;
    	}
    	V1Job latestJob = jobResp.getObject();
		if(latestJob != null && latestJob.getStatus() != null 
				&& latestJob.getStatus().getSucceeded() != null && latestJob.getStatus().getSucceeded() == 1) { 
			System.out.println("Pi Job succeeded.");
			break;
		}
		System.out.println("Job does not succeed. Wait for 2s");
    }
    
    // Delete the job
    jobClient.delete(piJob.getMetadata().getNamespace(), piJob.getMetadata().getName()).onFailure(
            errorStatus -> {
                System.out.println("Not Deleted!");
                throw new RuntimeException(errorStatus.toString());
              });
    System.out.println("Deleted!");
  }
}
