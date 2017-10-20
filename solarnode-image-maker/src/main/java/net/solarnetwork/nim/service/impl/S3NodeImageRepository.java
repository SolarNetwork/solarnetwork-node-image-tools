/* ==================================================================
 * S3NodeImageRepository.java - 21/10/2017 11:51:01 AM
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

package net.solarnetwork.nim.service.impl;

import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.util.TaskStepTracker;

/**
 * {@link NodeImageRepository} backed by Amazon S3 storage.
 * 
 * @author matt
 * @version 1.0
 */
public class S3NodeImageRepository extends AbstractNodeImageRepository
    implements UpdatableNodeImageRepository {

  @Override
  public Iterable<SolarNodeImageInfo> findAll() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SolarNodeImage findOne(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SolarNodeImage findOneCompressed(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SolarNodeImage save(SolarNodeImage image, TaskStepTracker tracker) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String id) {
    // TODO Auto-generated method stub

  }

}
