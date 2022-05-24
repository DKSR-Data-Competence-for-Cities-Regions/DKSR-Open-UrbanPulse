package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.urbanpulsecontroller.admin.CategoryManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.CyclicDependencyException;
import de.urbanpulse.urbanpulsecontroller.admin.ReferencedEntityMissingException;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryWithChildrenTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.CategoryRestFacade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.json.JsonObject;
import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static de.urbanpulse.urbanpulsemanagement.services.AbstractRestService.HTTP_STATUS_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryRestServiceTest {

    private static final String RELATIVE_PATH_FOR_ROOT = "/categories";
    private static final String CATEGORY_ID = "42";
    private static final String SENSOR_ID = "13";
    private static final String RELATIVE_PATH_FOR_ITEM = RELATIVE_PATH_FOR_ROOT + "/" + SENSOR_ID;
    private static final URI ABSOLUTE_PATH = URI.create("https://foo.bar/some/absolute/path/with/https/");
    private static final URI EXPECTED_LOCATION = URI.create("https://foo.bar/categories/4711");

    private static final String CATEGORY_NAME = "myLittleMockCategory";
    private static final String CATEGORY_DESC = "this is my little MockCategory";
    @Mock
    CategoryRestFacade mockCategoryRestFacade;
    @Mock
    Response mockResponse;
    @InjectMocks
    private CategoryRestService categoryRestService;
    @Mock(name = "sensorDao")
    private SensorManagementDAO mockSensorDao;
    @Mock(name = "categoryDao")
    private CategoryManagementDAO mockCategoryDao;
    @Mock
    private UriInfo mockContext;
    @Mock
    private UriBuilder mockUriBuilder;
    @Mock
    private SensorTO mockSensorTO;
    @Mock
    private CategoryEntity mockCategoryEntity;
    @Mock
    private CategoryTO mockCategoryTO;
    @Mock
    private CategoryWithChildrenTO mockCategoryWithChildrenTO;
    @Mock
    private JsonObject mockJsonObject;
    @Mock
    private io.vertx.core.json.JsonObject mockJsonObjectVertx;

    @Test
    public void getCategories_returnsCategoriesForSensor_ifGiven() {
        final String mockCategoryID = "23";
        List<String> mockCategories = new ArrayList<>();
        mockCategories.add(mockCategoryID);

        given(mockCategoryDao.getById(mockCategoryID)).willReturn(mockCategoryTO);
        given(mockSensorDao.getById(SENSOR_ID)).willReturn(mockSensorTO);
        given(mockSensorTO.getCategories()).willReturn(mockCategories);

        given(mockCategoryTO.toJson()).willReturn(mockJsonObject);

        Response response = categoryRestService.getCategories(SENSOR_ID, null, false, false);
        JsonObject foundCategories = (JsonObject) response.getEntity();

        verify(mockCategoryDao, never()).getAll();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(foundCategories.containsKey("categories"));
    }

    @Test
    public void getCategories_returnsCategoriesForSensor_ifNoSensorIdGiven() {
        List<CategoryTO> mockCategories = new ArrayList<>();
        mockCategories.add(mockCategoryTO);

        given(mockCategoryDao.getFilteredBy("name", CATEGORY_NAME)).willReturn(mockCategories);

        given(mockCategoryTO.toJson()).willReturn(mockJsonObject);

        Response response = categoryRestService.getCategories(null, CATEGORY_NAME, false, false);
        JsonObject foundCategories = (JsonObject) response.getEntity();

        verify(mockCategoryDao, never()).getAll();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getCategories_returnsAllCategoriesForSensor_Success() {
        List<CategoryTO> mockCategories = new ArrayList<>();
        mockCategories.add(mockCategoryTO);

        given(mockCategoryDao.getAll()).willReturn(mockCategories);

        given(mockCategoryTO.toJson()).willReturn(mockJsonObject);

        Response response = categoryRestService.getCategories(null, null, false, false);

        verify(mockCategoryDao, times(1)).getAll();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getCategories_returnsCategoriesForSensor_Name_And_SensorId_Given() {
        Response response = categoryRestService.getCategories(SENSOR_ID, CATEGORY_NAME, false, false);
        JsonObject badRequest = (JsonObject) response.getEntity();

        verify(mockCategoryDao, never()).getAll();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(badRequest);
    }

    @Test
    public void getCategories_returnsCategoriesForSensor_resolveChildren_And_No_OnlyRoot_given() {
        Response response = categoryRestService.getCategories(null, null, null, true);
        JsonObject badRequest = (JsonObject) response.getEntity();

        verify(mockCategoryDao, never()).getAll();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(badRequest);
    }

    @Test
    public void getCategoryById_returnsExpected() {
        given(mockCategoryDao.getById(CATEGORY_ID)).willReturn(mockCategoryTO);
        given(mockCategoryTO.toJson()).willReturn(mockJsonObject);
        given(mockJsonObject.toString()).willReturn("validJsonString");

        Response response = categoryRestService.getCategoryById(CATEGORY_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getCategoryById_returnNotFound() {

        Response response = categoryRestService.getCategoryById(CATEGORY_ID);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void getParentCategoryById_returnsExpected() {
        given(mockCategoryDao.getParentCategory(CATEGORY_ID)).willReturn(mockCategoryTO);
        given(mockCategoryTO.toJson()).willReturn(mockJsonObject);
        given(mockJsonObject.toString()).willReturn("validJsonString");

        Response response = categoryRestService.getParentCategory(CATEGORY_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getParentCategoryById_returnsNotFound() {

        Response response = categoryRestService.getParentCategory(CATEGORY_ID);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void createCategory_throwsUnprocEntityIfNameMissing() {
        Response response = categoryRestService.createCategory("{}", mockContext, mockCategoryRestFacade);
        JsonObject badRequest = (JsonObject) response.getEntity();

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
        assertNotNull(badRequest);
    }

    @Test
    public void createCategory_throwsUnprocEntityIfDescriptionMissing() {
        String Json = "{\"name\":\"gee\"}";

        Response response = categoryRestService.createCategory(Json, mockContext, mockCategoryRestFacade);
        JsonObject badRequest = (JsonObject) response.getEntity();

        assertEquals(HTTP_STATUS_UNPROCESSABLE_ENTITY, response.getStatus());
        assertNotNull(badRequest);
    }

    @Test
    public void createCategory_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException {
        final String Json = "{\"name\":\"" + CATEGORY_NAME + "\",\"description\":{\"name\":\"desc\"}}";

        List<CategoryTO> mockCategories = new ArrayList<>();
        given(mockCategoryDao.getFilteredBy("name", CATEGORY_NAME)).willReturn(mockCategories);
        given(mockCategoryDao.createCategory(CATEGORY_NAME, "{\"name\":\"desc\"}", new LinkedList<>())).willReturn((mockCategoryTO));
        given(mockCategoryTO.getId()).willReturn("4711");

        given(mockContext.getBaseUriBuilder()).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(any(Class.class))).willReturn(mockUriBuilder);
        given(mockUriBuilder.path(anyString())).willReturn(mockUriBuilder);
        given(mockUriBuilder.build()).willReturn(EXPECTED_LOCATION);

        Response response = categoryRestService.createCategory(Json, mockContext, mockCategoryRestFacade);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void createCategory_BadRequestIfNameAlreadyExist() throws ReferencedEntityMissingException, CyclicDependencyException {
        final String Json = "{\"name\":\"" + CATEGORY_NAME + "\",\"description\":{\"name\":\"desc\"}}";

        List<CategoryTO> mockCategories = new ArrayList<>();
        mockCategories.add(mockCategoryTO);

        given(mockCategoryDao.getFilteredBy("name", CATEGORY_NAME)).willReturn(mockCategories);
        Response response = categoryRestService.createCategory(Json, mockContext, mockCategoryRestFacade);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateCategory_throwsBadRequestIfNameMissing() {
        Response response = categoryRestService.updateCategory(CATEGORY_ID, "{}");
        JsonObject badRequest = (JsonObject) response.getEntity();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(badRequest);
    }

    @Test
    public void updateCategory_throwsBadRequestIfDescriptionMissing() {
        String Json = "{\"name\":\"gee\"}";

        Response response = categoryRestService.updateCategory(CATEGORY_ID, Json);
        JsonObject badRequest = (JsonObject) response.getEntity();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertNotNull(badRequest);
    }

    @Test
    public void updateCategory_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException, OperationNotSupportedException {
        final String Json = "{\"name\":\"" + CATEGORY_NAME + "\",\"description\":{\"name\":\"desc\"}}";

        given(mockCategoryDao.getById(CATEGORY_ID)).willReturn(mockCategoryTO);

        Response response = categoryRestService.updateCategory(CATEGORY_ID, Json);

        verify(mockCategoryDao, times(1)).updateCategory(mockCategoryTO, CATEGORY_NAME, "{\"name\":\"desc\"}", new LinkedList<>());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteCategory_returnsOkWithNameOfDeletedEvent() {
        given(mockCategoryDao.deleteByIdWithDependencies(SENSOR_ID)).willReturn(SENSOR_ID);

        Response response = categoryRestService.deleteCategory(SENSOR_ID);

        verify(mockCategoryDao, times(1)).deleteByIdWithDependencies(SENSOR_ID);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteSensor_returnsBadRequest_ifNotFound() {
        given(mockCategoryDao.deleteByIdWithDependencies(SENSOR_ID)).willReturn(SENSOR_ID);

        Response response = categoryRestService.deleteCategory(SENSOR_ID);

        verify(mockCategoryDao, times(1)).deleteByIdWithDependencies(SENSOR_ID);
        // per rest_concept.txt: we do want delete of a non-existing item to report success!
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getChildCategories_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException {
        List<CategoryWithChildrenTO> children = new ArrayList<>();
        children.add(mockCategoryWithChildrenTO);

        given(mockCategoryDao.getById(CATEGORY_ID)).willReturn(mockCategoryTO);
        given(mockCategoryDao.getFullyResolvedChildCategories(mockCategoryTO)).willReturn(children);
        given(mockCategoryWithChildrenTO.toJson()).willReturn(mockJsonObjectVertx);

        Response response = categoryRestService.getChildCategories(CATEGORY_ID, true);

        verify(mockCategoryDao, times(1)).getFullyResolvedChildCategories(mockCategoryTO);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getRootCategories_returnsExpected() throws ReferencedEntityMissingException, CyclicDependencyException {
        List<CategoryWithChildrenTO> children = new ArrayList<>();
        children.add(mockCategoryWithChildrenTO);

        given(mockCategoryDao.getFullyResolvedRootCategories()).willReturn(children);
        given(mockCategoryWithChildrenTO.toJson()).willReturn(mockJsonObjectVertx);

        Response response = categoryRestService.getRootCategories(true);

        verify(mockCategoryDao, times(1)).getFullyResolvedRootCategories();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Verify the response is valid json
        assertTrue(response.getEntity() instanceof String);
        String jsonString = (String) response.getEntity();
        io.vertx.core.json.JsonObject jsonObject = new io.vertx.core.json.JsonObject(jsonString);
        assertTrue(jsonObject.containsKey("categories"));
        io.vertx.core.json.JsonArray categories = jsonObject.getJsonArray("categories");
        assertEquals(1, categories.size());
        assertTrue(categories.getValue(0) instanceof io.vertx.core.json.JsonObject);
    }
}
