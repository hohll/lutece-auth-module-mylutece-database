/*
 * Copyright (c) 2002-2021, City of Paris
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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.mylutece.modules.database.authentication.service.DatabasePlugin;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.util.password.IPasswordFactory;
import jakarta.inject.Inject;

/**
 * The data of a user are read with his exact login: _ and % of a login are not wildcards
 */
public class DatabaseHomeLoginTest extends LuteceTestCase
{
    private static final String ROLE = "role_au10";

    @Inject
    private IPasswordFactory _passwordFactory;
    private Plugin _plugin;
    private DatabaseUser _userAbc;
    private DatabaseUser _userUnderscore;

    @BeforeEach
    protected void createUsers( )
    {
        _plugin = PluginService.getPlugin( DatabasePlugin.PLUGIN_NAME );
        String strPrefix = "au10" + System.nanoTime( );
        _userAbc = createUser( strPrefix + "abc" );
        _userUnderscore = createUser( strPrefix + "a_c" );
        DatabaseHome.addRoleForUser( _userAbc.getUserId( ), ROLE, _plugin );
        DatabaseHome.addGroupForUser( _userAbc.getUserId( ), "group_au10", _plugin );
    }

    @AfterEach
    protected void removeUsers( )
    {
        DatabaseHome.removeRolesForUser( _userAbc.getUserId( ), _plugin );
        DatabaseHome.removeGroupsForUser( _userAbc.getUserId( ), _plugin );
        DatabaseUserHome.remove( _userAbc, _plugin );
        DatabaseUserHome.remove( _userUnderscore, _plugin );
    }

    @Test
    public void testLoginWithUnderscoreGetsOnlyItsOwnData( )
    {
        assertEquals( List.of( ROLE ), DatabaseHome.findUserRolesFromLogin( _userAbc.getLogin( ), _plugin ) );
        assertTrue( DatabaseHome.findUserRolesFromLogin( _userUnderscore.getLogin( ), _plugin ).isEmpty( ) );
        assertTrue( DatabaseHome.findUserGroupsFromLogin( _userUnderscore.getLogin( ), _plugin ).isEmpty( ) );
        assertEquals( _userUnderscore.getUserId( ), DatabaseHome.findUserIdFromLogin( _userUnderscore.getLogin( ), _plugin ) );
        assertEquals( -1, DatabaseHome.findUserIdFromLogin( _userUnderscore.getLogin( ).replace( "_", "%" ), _plugin ) );
    }

    private DatabaseUser createUser( String strLogin )
    {
        DatabaseUser user = new DatabaseUser( );
        user.setLogin( strLogin );
        user.setFirstName( strLogin );
        user.setLastName( strLogin );
        user.setEmail( strLogin + "@example.org" );
        DatabaseUserHome.create( user, _passwordFactory.getPasswordFromCleartext( strLogin ), _plugin );
        return user;
    }
}
