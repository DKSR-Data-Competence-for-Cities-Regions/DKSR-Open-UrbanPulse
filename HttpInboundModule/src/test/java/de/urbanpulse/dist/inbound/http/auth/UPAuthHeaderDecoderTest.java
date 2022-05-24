package de.urbanpulse.dist.inbound.http.auth;

import io.vertx.core.logging.*;
import static org.hamcrest.CoreMatchers.is;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPAuthHeaderDecoderTest {
  UPAuthHeaderDecoder decoder;
  @Before
  public void setup(){
    decoder = new UPAuthHeaderDecoder();
  }
  private Logger LOGGER = LoggerFactory.getLogger(UPAuthHeaderDecoderTest.class);

  @Test
  public void testGetDecodedUsername(){
    String username = "UP p1r0c4:1440";
    try{
      String decoded = decoder.getDecodedUsername(username);
      LOGGER.info("DECODED: "+decoded);
    } catch (Exception e){
      LOGGER.error("SEVERE: Exception caught. "+username+" is a invalid username -> ",e);
    }
  }
  @Test
  public void testGetDecodedUsernameThrowIllegalArg(){
    String username = "UPbase1:base2";
    try{
      String decoded = decoder.getDecodedUsername(username);
      fail();
    } catch (Exception e){
      assertThat(e.getMessage(), is("failed to parse auth header "+username));
    }
  }
  @Test
  public void testGetDecodedUsernameThrowNullArg(){
    try{
      String decoded = decoder.getDecodedUsername(null);
      fail();
    } catch (Exception e){
      assertThat(e.getMessage(), is("failed to parse auth header null"));
    }
  }
  @Test
  public void testGetHash(){
    String authHeader = "UP base1:base2";
    try{
      String decoded = decoder.getHash(authHeader);
    } catch (Exception e){
      LOGGER.error("SEVERE: Exception caught. "+authHeader+"is a invalid username -> ",e);
    }
  }
  @Test
  public void testGetHashThrowIllegalArg(){
    String authHeader = "UP base1base2";
    try{
      String decoded = decoder.getHash(authHeader);
      fail();
    } catch (Exception e){
      assertThat(e.getMessage(), is("failed to parse auth header "+authHeader));
    }
  }
  @Test
  public void testGetHashThrowNullArg(){
    try{
      String decoded = decoder.getHash(null);
      fail();
    } catch (Exception e){
      assertThat(e.getMessage(), is("failed to parse auth header null"));
    }
  }
}
