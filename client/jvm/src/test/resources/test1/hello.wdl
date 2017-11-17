workflow Hello {
    String greeting

    call hello{
        input: world = greeting
    }

}

task hello {

    String world

    command {
        echo ${world} > 1.txt
    }

    runtime {
        docker: "ubuntu:latest"
    }

    output {
        File out = "1.txt"
    }

}
