/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.ext.javax.sip.dns;

import java.util.Comparator;

import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class SRVRecordComparator implements Comparator<Record> {

	public int compare(Record o1, Record o2) {
		SRVRecord o1SRVRecord = (SRVRecord) o1;
		SRVRecord o2SRVRecord = (SRVRecord) o2;
		int o1Priority = o1SRVRecord.getPriority();
		int o2Priority = o2SRVRecord.getPriority();
		// the lower priority is the best
		if(o1Priority > o2Priority)
			return 1;
		if(o1Priority < o2Priority)
			return -1;
		
		// if they are the same sort them through weight
		int o1Weight = o1SRVRecord.getWeight();
		int o2Weight = o2SRVRecord.getWeight();
		// the higher weight is the best
		if(o1Weight < o2Weight)
			return 1;
		if(o1Weight > o2Weight)
			return -1;
		// RFC 3263 Section 4.4
		return o1SRVRecord.getTarget().compareTo(o2SRVRecord.getTarget());
	}

}
