# Introduction #

The are the steps needed to configure this module for use in Alfresco Web Client.


# Details #

In the file web.xml of the Alfresco war:

1. Comment the Authentication filter used for Web Client:

```
  <!--                                                                                                            
  <filter-mapping>                                                                                                
     <filter-name>Authentication Filter</filter-name>                                                             
     <url-pattern>/faces/*</url-pattern>                                                                          
  </filter-mapping>                                                                                               
  -->
```

2. Declare the Container Security filter:

```
   <filter>
      <filter-name>Container Security Filter</filter-name> <filter-class>com.pararede.alfresco.security.AlfrescoContainerSecurityFilter</filter-class>
   </filter> 
```

3. Map the Container Security filter to url pattern used by Auhentication Filter

```
   <filter-mapping>
      <filter-name>Container Security Filter</filter-name>
      <url-pattern>/faces/*</url-pattern>
   </filter-mapping>
```

4. Configure the Security Constraint (any standard auth-method can be used):

```
   <security-constraint>
        <web-resource-collection>
            <web-resource-name>All</web-resource-name>
            <url-pattern>/faces/*</url-pattern>
        </web-resource-collection>
        <auth-constraint>
            <role-name>your role</role-name>
        </auth-constraint>
   </security-constraint>
		
   <login-config>
        <auth-method>BASIC</auth-method>
        <realm-name>your realm</realm-name>
   </login-config>
```

5. Try it in Alfresco Web Client.