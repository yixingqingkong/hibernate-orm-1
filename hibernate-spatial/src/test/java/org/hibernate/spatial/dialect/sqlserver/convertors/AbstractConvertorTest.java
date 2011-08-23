/*
 * $Id: AbstractConvertorTest.java 278 2010-12-18 14:03:32Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */

package org.hibernate.spatial.dialect.sqlserver.convertors;

import com.vividsolutions.jts.geom.Geometry;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
public class AbstractConvertorTest {

    private final static String TEST_DATA = "sqlserver/sqlserver-2008-test-data.ser";

    List<ConvertorTestData> testData = readTestData();
	Map<Integer, Geometry> decodedGeoms = new HashMap<Integer, Geometry>();
	Map<Integer, Object> rawResults;
	Map<Integer, byte[]> encodedGeoms;
	Map<Integer, Geometry> expectedGeoms;



    public List<ConvertorTestData> readTestData(){
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(TEST_DATA);
        if (in == null) {
            throw new RuntimeException("Can't find file " + TEST_DATA);
        }
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            return (List<ConvertorTestData>)oin.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // nothing to do
            }
        }
    }


	public void doDecoding(OpenGisType type) {
        List<ConvertorTestData> testData = readTestData();
        rawResults = toRawResults(testData, type.toString());
        expectedGeoms = toExpected(testData, type.toString());
		for ( Integer id : rawResults.keySet() ) {
			Geometry geometry = Decoders.decode( (byte[]) rawResults.get( id ) );
			decodedGeoms.put( id, geometry );
		}
	}

    private Map<Integer, Geometry> toExpected(List<ConvertorTestData> testData, String type) {
        Map<Integer, Geometry> result = new HashMap<Integer, Geometry>();
        for (ConvertorTestData item : testData) {
            if (! item.type.equals(type)) continue;
            result.put(item.id, item.geometry);
        }
        return result;
    }

    private Map<Integer, Object> toRawResults(List<ConvertorTestData> testData, String type) {
        Map<Integer, Object> result = new HashMap<Integer, Object>();
        for (ConvertorTestData item : testData) {
            if (! item.type.equals(type)) continue;
            result.put(item.id, item.bytes);
        }
        return result;
    }

    public void doEncoding() {
		encodedGeoms = new HashMap<Integer, byte[]>();
		for ( Integer id : decodedGeoms.keySet() ) {
			Geometry geom = decodedGeoms.get( id );
			byte[] bytes = Encoders.encode( geom );
			encodedGeoms.put( id, bytes );
		}
	}

	public void test_encoding() {
		for ( Integer id : encodedGeoms.keySet() ) {
			assertTrue(
					"Wrong encoding for case " + id,
					Arrays.equals( (byte[]) rawResults.get( id ), encodedGeoms.get( id ) )
			);
		}
	}

	public void test_decoding() {
		for ( Integer id : decodedGeoms.keySet() ) {
			Geometry expected = expectedGeoms.get( id );
			Geometry received = decodedGeoms.get( id );
			assertTrue( "Wrong decoding for case " + id, expected.equalsExact( received ) );
		}
	}
}
