<%-- Former link of the account validation e-mails: the validation is now the view validateAccount of the XPage mylutecedatabase --%>
${ pageContext.response.sendRedirect( '../../../../Portal.jsp?page=mylutecedatabase&view=validateAccount&key='.concat( empty param.key ? '' : param.key ) ) }
