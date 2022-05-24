package de.urbanpulse.cep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventBean;
import com.espertech.esper.event.bean.BeanEventType;
import io.vertx.core.json.JsonArray;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class EventBeanConverterTest {

    private EventBeanConverter eventBeanConverter;
    private BeanEventBean eventBean;

    @Before
    public void setup(){
        eventBeanConverter = new EventBeanConverter();
        String[] propertyNames = new String[]{"test"};

        BeanEventType beanEventTypeMock = Mockito.mock(BeanEventType.class);
        EventPropertyGetter mockedEventPropertyGetter = Mockito.mock(EventPropertyGetter.class);
        eventBean = new BeanEventBean("{\"test\": 123}", beanEventTypeMock);

        Mockito.doReturn(propertyNames).when(beanEventTypeMock).getPropertyNames();
        Mockito.doReturn(mockedEventPropertyGetter).when(beanEventTypeMock).getGetter(Mockito.anyString());
        Mockito.doReturn(123).when(mockedEventPropertyGetter).get(Mockito.any());
    }

    @Test
    public void toJsonArray_VSResult() {
        EventBean[] eventBeans = new EventBean[]{eventBean};
        JsonArray jsonArray = eventBeanConverter.toJsonArray(eventBeans,"test_VSResultStatement");

        assertEquals(1,jsonArray.size());
        assertEquals(123L, jsonArray.getJsonObject(0).getLong("test").longValue());
        assertEquals("test",jsonArray.getJsonObject(0).getJsonObject("_headers").getString("eventType"));
    }


    @Test
    public void toJsonArray() {
        EventBean[] eventBeans = new EventBean[]{eventBean};
        JsonArray jsonArray = eventBeanConverter.toJsonArray(eventBeans,"test");

        assertEquals(1,jsonArray.size());
        assertEquals(123L, jsonArray.getJsonObject(0).getLong("test").longValue());
        assertFalse(jsonArray.getJsonObject(0).containsKey("_headers"));
    }
}
