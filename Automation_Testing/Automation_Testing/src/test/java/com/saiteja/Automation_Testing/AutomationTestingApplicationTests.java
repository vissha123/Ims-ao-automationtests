package com.saiteja.Automation_Testing;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.saiteja.Automation_Testing.group.Group;
import com.saiteja.Automation_Testing.group.UserAdd;

@SpringBootTest
class AutomationTestingApplicationTests {
	
	@Autowired
	private Group group;
	
	@Autowired
	private UserAdd userAdd;
	
	

//  @Test
//  @Order(1)
//  void testGroupAddRequest() throws Exception {
//	  Group.connect();
//      String response = group.groupAdd();
//      System.out.println(response);
//      assertEquals("Group found", response);
//  }  
//  @Test
//  public void getRequestAutomation() {
//      
//      RestAssured.baseURI = "http://localhost:8891";
//
//      // Define the path variable value
//      String vkValue = "VK141415";
//
//      // Expected response
//      String expectedResponse = "Group found";
//
//      // Send the GET request with the path variable and capture the response
//      Response response = RestAssured.given()
//          .pathParam("input_vk", vkValue)  
//          .when()
//          .get("/GetGroup/{input_vk}"); 
//
//      
//      response.then().log().all();
//
//      // Extract the response body as a String
//      String actualResponse = response.getBody().asString();
//
//      assertEquals(200, response.getStatusCode());
//
//      assertEquals(expectedResponse, actualResponse);
//  }

	@Test
    public void groupAddTest() {
        try {
        	Group.connect();
            String result = group.groupAdd();
            if (result.contains("Group found")) {
                assertTrue(true, "Group Add is successful");
            } else {
                fail("Group add failed with message: " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred: " + e.getMessage());
        }
	}
	
	@Test
	public void userAddTest() {
		try {
			UserAdd.connect();
			String result = userAdd.userAddRequest();
			if(result.contains("User found")) {
				assertTrue(true, "User Add is successful");
			} else {
				fail("User add failed with message: " + result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception occured: " + e.getMessage());
		}
	}

}
