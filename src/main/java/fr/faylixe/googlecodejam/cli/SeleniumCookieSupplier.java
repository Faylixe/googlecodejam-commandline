package fr.faylixe.googlecodejam.cli;

import java.util.function.Supplier;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import fr.faylixe.googlecodejam.client.executor.Request;

/**
 * TODO Javadoc
 * TODO Cookie expiration parsing
 * TODO Login url as parameter ? Function instead of supplier ?.
 * 
 * ISSUE : https://github.com/Faylixe/googlecodejam-client/issues/5
 * @author fv
 */
public final class SeleniumCookieSupplier implements Supplier<String> {

	/** Name of the target cookie to retrieve. **/
	private static final String COOKIE_NAME = "SACSID";

	/** Initial login URL to navigate to with web driver. **/
	private static final String LOGIN_URL = "https://www.google.com/accounts/ServiceLogin?service=ah&passive=true&continue=https://appengine.google.com/_ah/conflogin%3Fcontinue%3D";

	/** Default waiting time between cookie check. **/
	private static final long WAITING_TIME = 2000;

	/** Property key for setting Selenium log4j status. **/
	private static final String LOGGING_PROPERTY = "org.apache.commons.logging.Log";

	/** Value of Selenium log4j status. **/
	private static final String LOGGING_VALUE = "org.apache.commons.logging.impl.Jdk14Logger";

	/** Target URL user should be redirected to. **/
	private final String target;

	/** Supplier that will create our driver instance to use. **/
	private final Supplier<WebDriver> driverSupplier;

	/** Lock object for notification exchange. **/
	private final Object lock;

	/** Boolean flag used for controlling {@link #waitForCookie(WebDriver)} method. **/
	private volatile boolean running;

	/** Retrieved cookie after l     ogin process. **/
	private Cookie result;

	/**
	 * Default constructor.
	 * 
	 * @param target Target URL user should be redirected to.
	 * @param driverSupplier Supplier that will create our driver instance to use.
	 */
	public SeleniumCookieSupplier(final String target, final Supplier<WebDriver> driverSupplier) {
		this.lock = new Object();
		this.target = target.replaceAll("/$", "");
		this.driverSupplier = driverSupplier;
	}

	/** {@inheritDoc} **/
	@Override
	public String get() {
		System.setProperty(LOGGING_PROPERTY, LOGGING_VALUE);
		final WebDriver driver = driverSupplier.get();
		final StringBuilder builder = new StringBuilder();
		builder
			.append(LOGIN_URL)
			.append(Request.getHostname())
			.append("/codejam");
		driver.navigate().to(builder.toString());
		running = true;
		waitForCookie(driver);
		return result == null ? null : result.getValue();
	}
	
	/**
	 * Indicates if the login process is still running.
	 * 
	 * @return <tt>true</tt> if the selenium instance is still running, <tt>false</tt> otherwise.
	 * @see #running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Cancel the current operation.
	 */
	public void cancel() {
		running = false;
	}

	/**
	 * Blocking method, that will wait until user has successfully
	 * logged in into Google Code Jam application through web driver.
	 */
	private void waitForCookie(final WebDriver driver) {
		while (isRunning()) {
			synchronized (lock) {
				try {
					lock.wait(WAITING_TIME);
					checkCurrentState(driver);
				}
				catch (final InterruptedException e) {
					break;
				}
			}
		}
		driver.quit();
	}
	
	/**
	 * Checks the state of the given <tt>driver</tt>,
	 * ensuring if the required cookie has been settled or not.
	 * 
	 * @param driver Driver to check state from.
	 */
	public void checkCurrentState(final WebDriver driver) {
		final String url = driver.getCurrentUrl().replaceAll("/$", "");
		if (target.equals(url)) {
			result = driver.manage().getCookieNamed(COOKIE_NAME);
			running = false;
		}
	}

}
