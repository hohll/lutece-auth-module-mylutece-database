/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.mylutece.modules.database.authentication.business;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import fr.paris.lutece.util.url.UrlItem;

/**
 * A search request missing some of the search fields searches the other fields and keeps the URL of the user list valid
 */
public class DatabaseUserFilterTest extends LuteceTestCase
{
    @Test
    public void testUrlAttributesWithMissingSearchFields( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.addParameter( "search_is_search", "true" );
        request.addParameter( "search_login", "jdoe" );

        DatabaseUserFilter filter = new DatabaseUserFilter( );
        assertTrue( filter.setDatabaseUserFilter( request ) );
        // the missing fields are empty, not null: the DAO searches LIKE '%%' (not '%null%') and the template prints them
        assertEquals( "jdoe", filter.getLogin( ) );
        assertEquals( "", filter.getLastName( ) );
        assertEquals( "", filter.getFirstName( ) );
        assertEquals( "", filter.getEmail( ) );

        UrlItem url = new UrlItem( "jsp/admin/plugins/mylutece/modules/database/ManageUsers.jsp" );
        filter.setUrlAttributes( url );
        String strUrl = url.getUrl( );
        assertTrue( strUrl.contains( "search_login=jdoe" ), strUrl );
        assertTrue( strUrl.contains( "search_last_name=" ), strUrl );

        String strAttributes = filter.getUrlAttributes( );
        assertTrue( strAttributes.contains( "search_login=jdoe" ), strAttributes );
        assertTrue( strAttributes.contains( "search_email=" ), strAttributes );
    }
}
