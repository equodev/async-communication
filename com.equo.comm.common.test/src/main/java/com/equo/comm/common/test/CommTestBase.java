package com.equo.comm.common.test;

import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.equo.chromium.swt.Browser;
import com.equo.comm.api.ICommService;
import com.equo.testing.common.osgi.base.BasicBrowserTest;

public class CommTestBase extends BasicBrowserTest {

	protected static final BundleContext context = FrameworkUtil.getBundle(CommTestBase.class).getBundleContext();

	protected static final String PWD = System.getProperty("user.dir") + "/";
	protected static final String RESOURCES_DIR = PWD + "src/main/resources/";

	private static ICommService getCommService() {
		ServiceReference<ICommService> svcref = context.getServiceReference(ICommService.class);
		Assert.assertNotNull(svcref);

		ICommService commService = context.getService(svcref);
		Assert.assertNotNull(commService);

		return commService;
	}

	protected static ICommService commService;

	protected void setFileResourceUrl(String resourcePath) {
		final Browser browser = (Browser) components.get(0);
		final Display display = Display.getDefault();
		display.syncExec(() -> {
			browser.setUrl("file://" + RESOURCES_DIR + resourcePath);
		});
	}

	@BeforeClass
	public static void initCommService() {
		commService = getCommService();
	}

	@Before
	public void waitForBrowser() {
		AtomicBoolean start = new AtomicBoolean(false);
		commService.on("_ready", runnable -> {
			start.set(true);
		});
		setFileResourceUrl("trigger_start.html");
		Awaitility.await().untilTrue(start);
	}

	protected static class TestPayload {
		private String id;
		private int number;
		private float fpNumber;
		private double dNumber;

		public TestPayload(String id, int number, float fpNumber, double dNumber) {
			this.id = id;
			this.number = number;
			this.fpNumber = fpNumber;
			this.dNumber = dNumber;
		}

		public double getdNumber() {
			return dNumber;
		}

		public void setdNumber(double dNumber) {
			this.dNumber = dNumber;
		}

		public float getFpNumber() {
			return fpNumber;
		}

		public void setFpNumber(float fpNumber) {
			this.fpNumber = fpNumber;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object otp) {
			if (!(otp instanceof TestPayload)) {
				return false;
			}
			TestPayload tp = (TestPayload) otp;
			return tp.id.equals(this.id) && tp.number == this.number && tp.fpNumber == this.fpNumber
					&& tp.dNumber == this.dNumber;
		}
	}

	protected void testSimpleSend(String url, Object somePayload) {
		AtomicBoolean success = new AtomicBoolean(false);
		getCommService().on("successTransferReceivePayload", (payload) -> {
			Assert.assertNull(payload);
			success.compareAndSet(false, true);
		});
		getCommService().on("failTransferReceivePayload", (payload) -> {
			// Try to fail faster than the timeout
			fail("Fail JSON");
		});
		getCommService().on("_startTransferReceivePayload", (payload) -> {
			Assert.assertNull(payload);
			getCommService().send("transfer", somePayload);
		});
		setFileResourceUrl(url);
		Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
	}

	protected void testSimpleReceive(String url, Object expectedPayload) {
		AtomicBoolean success = new AtomicBoolean(false);
		getCommService().on("transferPayload", (payload) -> {
			Assert.assertNotNull(payload);
			Assert.assertEquals(expectedPayload, payload);
			success.compareAndSet(false, true);
		});
		getCommService().on("_startTransferSendPayload", (payload) -> {
			Assert.assertNull(payload);
			getCommService().send("transfer");
		});
		setFileResourceUrl(url);
		Awaitility.await().timeout(Duration.ofSeconds(2)).untilTrue(success);
	}

	protected void testSimpleResponse(String url, String somePayload) {
		AtomicBoolean success = new AtomicBoolean(false);
		getCommService().on("successTransferReceiveResponse", (payload) -> {
			Assert.assertNull(payload);
			success.compareAndSet(false, true);
		});
		getCommService().on("failTransferReceiveResponse", (payload) -> {
			// Try to fail faster than the timeout
			fail();
		});
		getCommService().on("_startTransferReceiveResponse", (payload) -> {
			Assert.assertNull(payload);
			return somePayload;
		});
		setFileResourceUrl(url);
		Awaitility.await().timeout(Duration.ofSeconds(1)).untilTrue(success);
	}

}
