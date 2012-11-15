/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.page;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.util.WebElementUtil;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class AbstractPage
{
   protected List<String> getErrors()
   {
      List<WebElement> errorSpans = getDriver().findElements(By.xpath("//span[@class='errors']"));
      return WebElementUtil.elementsToText(errorSpans);
   }

   public enum PageContext
   {
      jsf, webTran
   }
   private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPage.class);

   private final WebDriver driver;
   private final FluentWait<WebDriver> ajaxWaitForTenSec;
   private List<WebElement> navMenuItems = Collections.emptyList();

   public AbstractPage(final WebDriver driver)
   {
      this(driver, PageContext.jsf);
   }

   public AbstractPage(final  WebDriver driver, PageContext pageContext)
   {
      PageFactory.initElements(new AjaxElementLocatorFactory(driver, 30), this);
      this.driver = driver;
      ajaxWaitForTenSec = waitForSeconds(driver, 10);
      if (pageContext == PageContext.jsf)
      {
         //webTran and jsp don't share same page layout
         navMenuItems = driver.findElement(By.className("navBar")).findElements(By.tagName("a"));
      }
   }

   public static FluentWait<WebDriver> waitForSeconds(WebDriver webDriver, int durationInSec)
   {
      return new FluentWait<WebDriver>(webDriver).withTimeout(durationInSec, SECONDS).pollingEvery(1, SECONDS).ignoring(NoSuchElementException.class);
   }

   public WebDriver getDriver()
   {
      return driver;
   }

   public String getTitle()
   {
      return driver.getTitle();
   }

   public List<String> getBreadcrumbs()
   {
      List<WebElement> breadcrumbs = driver.findElement(By.id("breadcrumbs_panel")).findElements(By.className("breadcrumbs_display"));
      return WebElementUtil.elementsToText(breadcrumbs);
   }

   public List<String> getNavigationMenuItems()
   {
      Collection<String> linkTexts = Collections2.transform(navMenuItems, new Function<WebElement, String>()
      {
         @Override
         public String apply(WebElement link)
         {
            return link.getText();
         }
      });
      return ImmutableList.copyOf(linkTexts);
   }

   public <P> P goToPage(String navLinkText, Class<P> pageClass)
   {
      LOGGER.info("click {} and go to page {}", navLinkText, pageClass.getName());
      List<String> navigationMenuItems = getNavigationMenuItems();
      int menuItemIndex = navigationMenuItems.indexOf(navLinkText);

      Preconditions.checkState(menuItemIndex >= 0, navLinkText + " is not available in navigation menu");

      navMenuItems.get(menuItemIndex).click();
      return PageFactory.initElements(driver, pageClass);
   }

   // TODO this doesn't seem useful
   public <P> P goToUrl(String url, P page)
   {
      LOGGER.info("go to url: {}", url);
      driver.get(url);
      PageFactory.initElements(new AjaxElementLocatorFactory(driver, 30), page);
      return page;
   }

   public FluentWait<WebDriver> waitForTenSec()
   {
      return ajaxWaitForTenSec;
   }
   
   protected void clickSaveAndCheckErrors(WebElement saveButton)
   {
      saveButton.click();
      List<String> errors = getErrors();
      if (!errors.isEmpty())
      {
         throw new RuntimeException(StringUtils.join(errors, ";"));
      }
   }
}
