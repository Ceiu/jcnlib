jcnlib - readme.txt
================================================================================



Introduction

    Welcome to jcnlib, the Java Chatnet library.
    
    jcnlib originally started as a very small package designed specifically so
    I didn't have to keep reimplementing the Chatnet backend to any cn-based
    projects I had in mind. Over the last few months, it has turned into the
    defacto standard library to use when making bots in Hyperspace.
    
    While I've made a fair amount of effort in making jcnlib easy to use, it's
    still expected that you have some degree of programming knowledge. If you've
    never used Java and/or have no programming experience, I highly recommend
    you read a tutorial before you even get started here. I don't much care for
    explaining how the examples work on a line-by-line basis.
    
    In any event, if you do happen to get stuck, you can either post a message
    on the forums or attempt to contact me in-game. Generally, I'd prefer if
    you'd post on the forums, as it gives others a chance to get quick answers
    to similar issues.
    
    The forums are currently hosted on subspace.co, but you can jump directly to
    the development forums with the following URL: 
    
        http://www.subspace.co/forum/4-development-projects/
   
    - Chris "Ceiu" Rog



Directory Structure

    The directory structure should follow the diagram below. If any files are
    missing, check that you got the latest package from the jcnlib forums at
    http://www.ssforum.net/index.php?showforum=382
    

    root
     |
     +- api
     |   +- (various .html files)
     |
     +- demos
     |   +- SampleBot
     |       +- src
     |           +- (various .java files)
     |   
     +- src
     |   +- (various .java files)
     |
     +- jcnlib_v2.0.1.###.jar
     |
     +- changelog.txt
     +- readme.txt
    
    
    The contents are as follows:
    
        - api/
            Contains the api documentation in javadoc form. If you want to know
            what functionality exists in the core and how to use it, this is
            where you look.
            
        - demo/
            Contains any pre-packaged demos. As of 1.5, this only includes
            SampleBot, which is designed to get you started with jcnlib. Other
            demos may be added in the future.

        - src/
            The jcnlib source. This is only for the diehards who want to
            customize the library or otherwise see how it works. Newbies should
            avoid this directory.

        - jcnlib_v2.0.1.###.jar
            The precompiled class files, in a pretty .jar file. The actual name
            of this file will change as the versions change, so just check that
            it starts with "jcnlib" and ends with "jar".
            
        - changelog.txt
            A list of changes made to the library.

        - readme.txt
            The file you're reading.



Installation/Basic Usage

    Since this is a Java library, there really isn't any installation to do in
    the traditional sense. Instead, you just have to make sure that it is
    included in your project's classpath when it starts.

    Of course, if you're using a decent IDE it will likely do this for you.



SampleBot Setup Guide

    Included with this package is the SampleBot demo, which demonstrates the
    very basics of jcnlib. This section will walk you through the process of
    setting up and running the SampleBot demo.
    
    1.  Before you can compile or run Java applications, you need to download
        and install Java. jcnlib requires Java 7 or newer, so grab the JDK
        package for Java SE 7 from here:
        
        http://www.oracle.com/technetwork/java/javase/downloads/index.html
        
    
    2.  Get yourself an IDE. I personally recommend JCreator which you can get
        for free at www.jcreator.com. You can use any IDE you'd like, though
        this tutorial assumes you're using JCreator. Eclipse is another solid
        alternative.
    
    3.  Choose a directory for your new bot. This directory will be referred to
        as the root directory from here on. The actual location of this
        directory doesn't matter, so long as you have read/write access to it
        and its subdirectories.
    
    4.  Copy the "src" directory from the SampleBot demo (included with jcnlib)
        into the root directory. Make sure the directory contains the files
        "exe.java" and "SampleBot.java."
        
        Next, create a new directory called "lib" in the root directory, then
        copy the jcnlib jar file into it, as well as any other jar files your
        project requires.
        
        When you're finished, your directory should look like the following:
        
        <root>
         |
         +- lib
         |   +- jcnlib_v2.0.1.###.jar
         |   +- (any other .jar files required by your project)
         |
         +- src
             +- BotActions.java
             +- exe.java
             +- (any other .java files that may be included in the demo)
    
    5.  Startup JCreator. Once it's done doing it's startup nonsense and brings
        up the main window, create a new project.
        
        - Click the File menu option at the top, then go to New->Project. This
          will bring up the New Project Wizard. 
        
        - Select "Empty Project" from the list, then click next.
        
        At this point the Project Wizard will be asking for a Name, Location,
        Source Path and Output Path. Set the name to "SampleBot" and the
        Location to the root directory you setup earlier. If all goes well,
        JCreator will automatically set the Source Path to <root>\src and the
        Output Path to <root>. If it didn't, you'll need to set them manually.
        
        Ex: If your root directory is "C:\samplebot", your project settings
            should be as follows:
            
            Name:           SampleBot
            Location:       C:\samplebot
            Source Path:    C:\samplebot\src
            Output Path:    C:\samplebot
        
        Once the directories are set, click Finish. Wait for JCreator to create
        the project space, then click Finish again.
        
    6.  If all went well, JCreator should automatically recognize the files in
        the 'lib' and 'src' directories. If so, it should be ready to compile.
        But before we do that, we need to give the bot a valid username and
        password.
        
        Think of a clever name for your bot, and create the new account with
        Continuum. If you don't do this, you won't be able to login with your
        bot, since Chatnet clients are not allowed to create new accounts.
        
    7.  Open the BotActions.java file (by double-clicking it in the File View
        panel) and look around line 35 for the following pieces of code:
        
          super.setHostConfig("208.122.59.226", 5005);
          super.setUserConfig("UB-SampleBot", "BOT PASSWORD HERE");
        
        Obviously these are no good, so you'll need to change the username and
        password to the username and password you just created. Additionally,
        if you don't intend on connecting to SSCE Hyperspace, the IP and port
        will also need to be changed.
        
    8.  Select "Build Project" from the Build menu option. If everything is
        working as expected, you should see "Process completed" in the Build
        Output window.
        
        *** In the event there are build errors, ensure JCreator is using the
        correct version of the JDK. To verify this, select "Project Settings"
        from the "Project" menu. In the "JDK Profiles" tab, you should have
        at least one entry that looks like this:
        
          "JDK version 1.7.0_04"
        
        If the version selected is less than 1.7, you'll be unable to compile
        the bot. If you're sure you've installed version 7 (or newer) of the
        JDK, you can simply add the profile by clicking "New..." and navigating
        to the JDK install directory (C:\Program Files\Java\ by default).

    9.  Once compiled, we're ready to run the project. Select "Run Project" from
        the Run menu. This should bring up a console window and your new bot
        should be running.
        
    10. Congratulations! You've successfully run your first jcnlib-based bot.
        You can now add new behaviors to the bot and make it do stuff. Take a
        look at BotActions.java for a few examples on handling events.
        
        Once you're ready to run your bot outside of your IDE, you can use the
        run_samplebot.bat batch file included with the SampleBot demo.
        
        If you have any other questions, leave a message on the forums.



Further Assistance
    
    If in the odd chance that all the documentation included with this package
    isn't enough to get you pointed in the right direction, leave a message on
    the forums and someone should reply within a few hours.
    
    http://www.subspace.co/forum/4-development-projects/
    