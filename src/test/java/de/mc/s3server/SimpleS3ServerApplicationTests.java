/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server;

import com.basho.riakcs.client.api.RiakCSClient;
import com.basho.riakcs.client.api.RiakCSException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SimpleS3ServerApplication.class)
@WebAppConfiguration
public class SimpleS3ServerApplicationTests {

	@Test
	public void contextLoads() {
	}


	@Test
	public void testListBuckets() throws UnknownHostException, RiakCSException {
		RiakCSClient client = new RiakCSClient("test" ,"test", "localhost:8080/api/s3/", false);
		assertEquals(3, client.listBuckets().getAsJsonArray("bucketList").size());

 	}



}
