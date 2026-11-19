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
package fr.paris.lutece.plugins.mylutece.modules.database.authentication.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;

/**
 * The screens of the module are MVC controllers whose actions are checked by the security token of the core
 */
public class MvcControllersSecurityTokenTest
{
    private static final Path TEMPLATES = Path.of( "webapp/WEB-INF/templates" );
    private static final Path JSP_ADMIN = Path.of( "webapp/jsp/admin/plugins/mylutece/modules/database" );
    private static final Path JSP_SITE = Path.of( "webapp/jsp/site/plugins/mylutece/modules/database" );
    private static final Pattern JSP_OF_THE_MODULE = Pattern.compile( "plugins/mylutece/modules/database/([A-Za-z]+\\.jsp)" );

    /**
     * Every action of the back office controllers is checked, and is given its token by a view (a form or a confirmation)
     */
    @Test
    public void testBackOfficeActionsHaveTheTokenOfAView( )
    {
        assertTrue( DatabaseJspBean.class.getAnnotation( fr.paris.lutece.portal.util.mvc.admin.annotations.Controller.class ).securityTokenEnabled( ) );
        assertTrue( GroupJspBean.class.getAnnotation( fr.paris.lutece.portal.util.mvc.admin.annotations.Controller.class ).securityTokenEnabled( ) );
        assertActionsHaveTheTokenOfAView( DatabaseJspBean.class, 13 );
        assertActionsHaveTheTokenOfAView( GroupJspBean.class, 6 );
    }

    /**
     * Every action of the front office page is checked, and is given its token by a view
     */
    @Test
    public void testFrontOfficeActionsHaveTheTokenOfAView( )
    {
        assertTrue( MyLuteceDatabaseApp.class.getAnnotation( fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller.class ).securityTokenEnabled( ) );
        assertActionsHaveTheTokenOfAView( MyLuteceDatabaseApp.class, 7 );
    }

    /**
     * No template links a JSP of the module other than the two controllers, and the only JSP of the front office is the redirection of the former
     * validation links
     */
    @Test
    public void testOnlyTheControllerJspRemain( ) throws IOException
    {
        assertEquals( Set.of( "ManageUsers.jsp", "ManageGroups.jsp" ), fileNames( JSP_ADMIN ) );
        assertEquals( Set.of( "DoValidateAccount.jsp" ), fileNames( JSP_SITE ) );

        List<Path> listTemplates;
        try ( Stream<Path> stream = Files.walk( TEMPLATES ) )
        {
            listTemplates = stream.filter( p -> p.toString( ).endsWith( ".html" ) ).collect( Collectors.toList( ) );
        }
        for ( Path template : listTemplates )
        {
            Matcher matcher = JSP_OF_THE_MODULE.matcher( Files.readString( template, StandardCharsets.UTF_8 ) );
            while ( matcher.find( ) )
            {
                assertTrue( Set.of( "ManageUsers.jsp", "ManageGroups.jsp" ).contains( matcher.group( 1 ) ), template + " links " + matcher.group( 1 ) );
            }
        }
    }

    /**
     * The former links of the front office page, naming a view with the action parameter, are actions without token that only redirect to the view
     * (the core checks the token of every action before the page is called)
     */
    @Test
    public void testFormerActionLinksAreRedirectedToTheirView( )
    {
        Set<String> setLegacy = new HashSet<>( );
        for ( Method method : MyLuteceDatabaseApp.class.getDeclaredMethods( ) )
        {
            Action action = method.getAnnotation( Action.class );
            if ( action != null && action.securityTokenDisabled( ) )
            {
                assertTrue( method.getName( ).startsWith( "legacy" ), method.getName( ) + " must only redirect a former link" );
                setLegacy.add( action.value( ) );
            }
        }
        assertEquals( Set.of( "accessDenied", "changePassword", "viewAccount", "lostPassword", "lostLogin", "createAccount", "modifyAccount",
                "reinitPassword", "reactivateAccount", "getResetPasswordPage", "delete", "deleteConfirm" ), setLegacy );
        assertEquals( setLegacy, legacyViews( MyLuteceDatabaseApp.class ) );
    }

    /**
     * The links built by the module name the views of the front office page with the view parameter: only the actions (do*) use the action parameter
     */
    @Test
    public void testLinksNameTheViewsWithTheViewParameter( ) throws IOException
    {
        Pattern actionLink = Pattern.compile( "page=mylutecedatabase&(?:amp;)?action=(?!do)([A-Za-z]+)" );
        List<Path> listFiles;
        try ( Stream<Path> stream = Stream.of( Path.of( "webapp" ), Path.of( "src/java" ), Path.of( "src/sql/plugins/mylutece/modules/database/core" ) )
                .flatMap( MvcControllersSecurityTokenTest::walk ) )
        {
            listFiles = stream.filter( p -> p.toString( ).matches( ".*\\.(html|properties|java|sql)$" ) ).collect( Collectors.toList( ) );
        }
        for ( Path file : listFiles )
        {
            Matcher matcher = actionLink.matcher( Files.readString( file, StandardCharsets.ISO_8859_1 ) );
            assertFalse( matcher.find( ), file + " names the view " + ( matcher.find( 0 ) ? matcher.group( 1 ) : "" ) + " with the action parameter" );
        }
    }

    private static Stream<Path> walk( Path folder )
    {
        try
        {
            return Files.walk( folder );
        }
        catch( IOException e )
        {
            throw new java.io.UncheckedIOException( e );
        }
    }

    private static Set<String> legacyViews( Class<?> controller )
    {
        Set<String> setViews = new HashSet<>( );
        for ( Method method : controller.getDeclaredMethods( ) )
        {
            View view = method.getAnnotation( View.class );
            if ( view != null )
            {
                setViews.add( view.value( ) );
            }
        }
        Set<String> setLegacy = new HashSet<>( );
        for ( Method method : controller.getDeclaredMethods( ) )
        {
            Action action = method.getAnnotation( Action.class );
            if ( action != null && action.securityTokenDisabled( ) && setViews.contains( action.value( ) ) )
            {
                setLegacy.add( action.value( ) );
            }
        }
        return setLegacy;
    }

    private static Set<String> fileNames( Path folder ) throws IOException
    {
        try ( Stream<Path> stream = Files.list( folder ) )
        {
            return stream.map( p -> p.getFileName( ).toString( ) ).collect( Collectors.toSet( ) );
        }
    }

    private static void assertActionsHaveTheTokenOfAView( Class<?> controller, int nActions )
    {
        Set<String> setTokenActions = new HashSet<>( );
        Set<String> setActions = new HashSet<>( );
        for ( Method method : controller.getDeclaredMethods( ) )
        {
            View view = method.getAnnotation( View.class );
            if ( view != null )
            {
                setTokenActions.add( view.securityTokenAction( ).isEmpty( ) ? view.value( ) : view.securityTokenAction( ) );
            }
            Action action = method.getAnnotation( Action.class );
            if ( action != null && action.securityTokenDisabled( ) && legacyViews( controller ).contains( action.value( ) ) )
            {
                // former link action=<view>: it only redirects to the view
                continue;
            }
            if ( action != null )
            {
                assertFalse( action.securityTokenDisabled( ), controller.getSimpleName( ) + "." + action.value( ) + " must check the token" );
                setActions.add( action.value( ) );
            }
        }
        assertEquals( nActions, setActions.size( ), controller.getSimpleName( ) + " " + setActions );
        for ( String strAction : setActions )
        {
            assertTrue( setTokenActions.contains( strAction ), controller.getSimpleName( ) + "." + strAction + " has no view giving its token" );
        }
        assertNotNull( setTokenActions );
    }
}
