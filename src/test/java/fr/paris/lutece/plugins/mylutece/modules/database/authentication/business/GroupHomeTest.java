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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.mylutece.modules.database.authentication.service.DatabasePlugin;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.util.password.IPasswordFactory;
import jakarta.inject.Inject;

/**
 * Removal and update of the database groups
 */
public class GroupHomeTest extends LuteceTestCase
{
    @Inject
    private IPasswordFactory _passwordFactory;

    /**
     * Removing a group removes its roles and its members, and only this group even when its key holds _
     */
    @Test
    public void testRemoveGroup( )
    {
        Plugin plugin = PluginService.getPlugin( DatabasePlugin.PLUGIN_NAME );
        String strPrefix = "au11" + System.nanoTime( );
        Group groupAbc = createGroup( strPrefix + "abc", plugin );
        Group groupUnderscore = createGroup( strPrefix + "a_c", plugin );
        GroupRoleHome.addRole( groupUnderscore.getGroupKey( ), "role_au11", plugin );
        GroupRoleHome.addRole( groupAbc.getGroupKey( ), "role_au11_abc", plugin );

        DatabaseUser user = new DatabaseUser( );
        user.setLogin( strPrefix );
        user.setFirstName( strPrefix );
        user.setLastName( strPrefix );
        user.setEmail( strPrefix + "@example.org" );
        DatabaseUserHome.create( user, _passwordFactory.getPasswordFromCleartext( strPrefix ), plugin );
        DatabaseHome.addGroupForUser( user.getUserId( ), groupUnderscore.getGroupKey( ), plugin );
        try
        {
            // updating the description of a_c does not touch abc
            groupUnderscore.setGroupDescription( "new description" );
            GroupHome.update( groupUnderscore, plugin );
            assertEquals( "description", GroupHome.findByPrimaryKey( groupAbc.getGroupKey( ), plugin ).getGroupDescription( ) );

            GroupHome.remove( groupUnderscore.getGroupKey( ), plugin );

            assertNull( GroupHome.findByPrimaryKey( groupUnderscore.getGroupKey( ), plugin ) );
            assertNotNull( GroupHome.findByPrimaryKey( groupAbc.getGroupKey( ), plugin ) );
            assertTrue( GroupRoleHome.findGroupRoles( groupUnderscore.getGroupKey( ), plugin ).isEmpty( ) );
            assertEquals( 1, GroupRoleHome.findGroupRoles( groupAbc.getGroupKey( ), plugin ).size( ) );
            assertTrue( DatabaseHome.findUserGroupsFromLogin( user.getLogin( ), plugin ).isEmpty( ) );
        }
        finally
        {
            DatabaseHome.removeGroupsForUser( user.getUserId( ), plugin );
            DatabaseUserHome.remove( user, plugin );
            GroupHome.remove( groupAbc.getGroupKey( ), plugin );
            GroupHome.remove( groupUnderscore.getGroupKey( ), plugin );
        }
    }

    private static Group createGroup( String strKey, Plugin plugin )
    {
        Group group = new Group( );
        group.setGroupKey( strKey );
        group.setGroupDescription( "description" );
        GroupHome.create( group, plugin );
        return group;
    }
}
