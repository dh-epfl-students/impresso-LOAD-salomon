$(document).ready(function() {

    var query=$("#query").val();//get the query value to call a ajax function and get the results
    var target=$("#hidden_target").val();
    var fp1 = new Fingerprint();
    //at first show the loading image
    $('#loading-image').removeClass("hidden")
// startPage();
    //ajax query is send for the graph to be created .
    $.ajax({url:"/query?query="+query+"&&target="+target+"&&user="+fp1.get(),
        beforeSend: function( xhr ) {
        }}).done(function(data) {
        json = JSON.parse( data );
        //show the table if the data is returned
        $("#table").removeClass("hidden");
        if(json.message)//if there are errors show them
        {
            $('#loading-image').addClass("hidden")
            $("#message").text(json.message);
            $("#message").removeClass("hidden");

        }
        //show the header that is specific to each of the categories
        //also load the  results in the specific format for each category
        if(json.date)
        {
            $("#header").text("Date");
            $("#date_filter").removeClass("hidden");
            $("#isDate").val("true");
            $("#date_header").removeClass("hidden");
            json.results.forEach(function(element) {
               $("#table tbody").append('  <tr  > <td class="dt-left"> <span><b class="name-label">'+element.label+
                   '</b> <input type="hidden" class="name-type" value='+element.type+'> <input type="hidden" class="name-id" value='+
                   element.node_id+'></span> </td> <td class="dt-right">'+element.score+'</td> /tr>')
            });
        }
        if(json.other) {
            if (json.loc) {
                $("#header").text("Locations");
            }
            if (json.act) {

                $("#header").text("Actors");
            }

            if (json.org) {
                $("#header").text("Organisations");
            }

            json.results.forEach(function(element){
                $("#datatable tbody").append('  <tr  > <td class="dt-left"> <span><b class="name-label">'+element.label+
                    '</b>&nbsp;&nbsp;(<a class="name-node-id" target="_blank" href="http://www.wikidata.org/entity/'+element.wikidata_id+
                    '">'+element.wikidata_id+'</a>) <input type="hidden" class="name-type" value='+element.type+'> <input type="hidden" class="name-id" value='+element.node_id+
                    '> </span> </td> <td class="dt-right">'+element.score+'</td> </tr>') ;
            });
        }
        if(json.term)
        {
            $("#header").text("Terms");
            json.results.forEach(function(element){
               $("#datatable tbody").append('  <tr  > <td class="dt-left"> <span><b class="name-label">'+element.label+
                   '</b><input type="hidden" class="name-type" value='+element.type+
                   '> <input type="hidden" class="name-id" value='+element.node_id+'></span> </td> <td class="dt-right">'+element.score+'</td> </tr>') ;
            });
        }

        if(json.page)
        {
            $("#header").text("Pages");
            json.results.forEach(function(element){
                $("#datatable tbody").append(' <tr  > <td class="dt-left"> <span><a href='+element.url+' target="_blank">'+element.title+
                    '</a> </span> </td> <td class="dt-right">'+element.score+'</td> </tr>') ;
            });
        }
        if(json.sentence)
        {
            $("#header").text("Sentences");
            json.results.forEach(function(element){
                $("#datatable tbody").append('<tr  ><td class="dt-left"><span>'+element.text+
                    '&nbsp;<a href='+element.pageURL+' > (source)</a>'+'</span> </td><td class="dt-right">'+element.score+'</td></tr>') ;
            });
        }
        //---------- fill the tagsinput
        $("span.cache-items").each(function(d)
        {        //the cache items come after a request to the server has been made

            //if there are cache items fill the text box with them
            var node_id = $(this).children("input.name-id").val();
            var node_id = $(this).children("input.name-id").val();
            var type = $(this).children("input.name-type").val();
            var label = $(this).children("input.name-label").val();
            $("#entity-search").tagsinput('add', {"node_id": node_id, "label": label, "type": type, "outside": true}
            );
            $(".entity").attr("placeholder","");
        })
        startPage();
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

function startPage()
{
    //--------to order the data on desc based on the score column and change the name of the search label
    $('#datatable').dataTable({
            "order": [[ 1, "desc" ]]
            ,
            "oLanguage": {
                "sSearch": "Search in results:"
            }
        }
    );
    //--------Limit to four decimal
    $(".dt-right").each(function (e) {
        var number=$(this).text();
        $(this).text(parseFloat(number).toLocaleString(undefined, { minimumFractionDigits: 4 }));

    });

    var table = $('#datatable').DataTable();//get the object of the bootstap datatable

    //--------Limit to four decimal on each page change as well

    table.on( 'draw.dt', function () {//on draw function will be called each time the table is drawn
        // so we parse the score column in this event and limit them to four decimals
        $(".dt-right").each(function (e) {
            var number=$(this).text();
            $(this).text(parseFloat(number).toLocaleString(undefined, { minimumFractionDigits: 4 }));

        });
    } );
    //--------Datatable click function
    $('#datatable tbody').on('click', 'tr', function () {
        //when double clicked on a row the term inside will be added to the tagsinput as a search termn
        var data = table.row( this ).data();
        //get the information about the row to create a tag input based on that
        if($(data[0]).children("b").text().length!=0) {
            var label = $(data[0]).children("b").text();//get the label
            if($(data[0]).children("input.name-id").text().length>0)
                var node_id = $(data[0]).children("input.name-id").text();///get the node id
            else
                var node_id = $(data[0]).children("input.name-id").val();
            var type = $(data[0]).children("input.name-type").val();//get the type
            // add the object
            //the attribute of outside=true is added so we know the data is not typed in the search box
            //but comes from the table or other sources
            //this is necessary for the tagsinput add function otherwise it will not behaive correctly
            $("#entity-search").tagsinput('add', {"node_id": node_id, "label": label, "type": type, "outside": true}
            );
        }

    } );
    if($("#isDate").val()=="true")
    {
        //--------year filter
        $.fn.dataTable.ext.search.push(
            //to add an extra filter to the bootstrap filters we use this
            //the filter is only added for the type date and limits the data to only year, months and year or months and year and day
            function( settings, data, dataIndex ) {
                var  filter=$('input[name=optradio]:checked').val();
                var date =  data[0].toString(); // use data for the date column

                if(filter=="year")
                {
                    return true;
                }
                else if(filter=="month")
                {
                    if(date.length>8)
                        return true;
                }
                else if(filter=="day")
                {
                    if(date.length>11)
                        return true;
                }
                return false;
            }
        );
        //Event listener to the filter
        $('input[type=radio][name=optradio]').change(function() {
            table.draw();
        });
    }

    // //---------- fill the tagsinput
    // $("span.cache-items").each(function(d)
    // {
    //     //if there are some items send with the cache then fill in the text box with them
    //     var node_id = $(this).children("input.name-id").val();
    //     var type = $(this).children("input.name-type").val();
    //     var label = $(this).children("input.name-label").val();
    //     $("#entity-search").tagsinput('add', {"node_id": node_id, "label": label, "type": type, "outside": true}
    //     );
    //     $(".entity").attr("placeholder","");//if there are cached items there is no need for a placeholder
    // })
}