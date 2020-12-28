$(document).ready(function() {

    var query=$("#query").val();//get the query value to call a ajax function and get the graph

    //at first show the loading image
    $('#loading-image').removeClass("hidden")
    var fp1 = new Fingerprint();
    //ajax query is send for the graph to be created .
    $.ajax({url:"/graph?query="+query+"&&user="+fp1.get(),
        beforeSend: function( xhr ) {
        }}).done(function(data) {
        //call the d3 function on the data
        json = JSON.parse( data );

        if(json.message)//if there are errors show them
        {
            $("#message").text(json.message);
            $("#message").removeClass("hidden");
            $('#loading-image').addClass("hidden")
        }
        else {
            graphStart(data);
        }
        //---------- fill the tagsinput
        $("span.cache-items").each(function(d)
        {        //the cache items come after a request to the server has been made

            //if there are cache items fill the text box with them
            var node_id = $(this).children("input.name-id").val();
            var type = $(this).children("input.name-type").val();
            var label = $(this).children("input.name-label").val();
            $("#entity-search").tagsinput('add', {"node_id": node_id, "label": label, "type": type, "outside": true}
            );
            $(".entity").attr("placeholder","");
        })
    }).error(function(data){
        //hide the loading image when the data is returned even if it is error
        $("loadingImg").addClass("hidden");
        if(json.messages)//if there are errors show them
        {
            $("#message").text(json.messages);
            $("#message").removeClass("hidden");

        }
        else {
            $("#message").text("An Unkwon Error has occured! ");
        }
        $("#message").removeClass("hidden");
    }).complete(function(data){
        //hide the loading image when the data is returned even if it is error
        $('#loading-image').addClass("hidden");
    });

    //---------- fill the tagsinput
    $("span.cache-items").each(function(d)
    {            //if there are cache items fill the text box with them
        var node_id = $(this).children("input.name-id").val();
        var type = $(this).children("input.name-type").val();
        var label = $(this).children("input.name-label").val();
        $("#entity-search").tagsinput('add', {"node_id": node_id, "label": label, "type": type, "outside": true}
        );
        $(".entity").attr("placeholder","");
    })

});
