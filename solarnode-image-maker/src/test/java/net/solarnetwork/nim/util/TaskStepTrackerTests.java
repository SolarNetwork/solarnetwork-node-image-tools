/* ==================================================================
 * TaskStepTracker.java - 20/10/2017 7:20:26 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.nim.util;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Test cases for the {@link TaskStepTracker} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TaskStepTrackerTests {

  @Test
  public void performWorkOneStep() {
    TaskStepTracker t = new TaskStepTracker(1);
    assertThat(t.getOverallPercentComplete(), closeTo(0.0, 0.001));
    for (int i = 1; i <= 5; i++) {
      double p = (double) i / (double) 5;
      t.setStepPercentComplete(p);
      assertThat(t.getOverallPercentComplete(), closeTo(p, 0.001));
    }
    t.completeStep();
    assertThat(t.getOverallPercentComplete(), closeTo(1.0, 0.001));
  }

  @Test
  public void performWorkMultiDiscreteSteps() {
    TaskStepTracker t = new TaskStepTracker(3);
    assertThat(t.getOverallPercentComplete(), closeTo(0.0, 0.001));
    for (int s = 1; s <= 3; s++) {
      t.completeStep();
      assertThat(t.getOverallPercentComplete(), closeTo((double) s / (double) 3, 0.001));
    }
    assertThat(t.getOverallPercentComplete(), closeTo(1.0, 0.001));
  }

  @Test
  public void performWorkMultiIncrementalSteps() {
    TaskStepTracker t = new TaskStepTracker(3);
    assertThat(t.getOverallPercentComplete(), closeTo(0.0, 0.001));
    double[] expectedPercents = new double[15];
    for (int i = 0; i < 15; i++) {
      expectedPercents[i] = (i + 1) * (1.0 / 3.0 / 5.0);
    }
    for (int s = 1; s <= 3; s++) {
      for (int i = 1; i < 5; i++) {
        double p = (double) i / (double) 5;
        t.setStepPercentComplete(p);
        int j = (s - 1) * 5 + (i - 1);
        assertThat(t.getOverallPercentComplete(), closeTo(expectedPercents[j], 0.001));
      }
      t.completeStep();
      assertThat(t.getOverallPercentComplete(), closeTo((double) s / (double) 3, 0.001));
    }
    assertThat(t.getOverallPercentComplete(), closeTo(1.0, 0.001));
  }

}
