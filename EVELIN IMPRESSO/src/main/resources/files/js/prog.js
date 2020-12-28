
$(document).ready(function() {
    var fp1 = new Fingerprint();

//-------------------------------tagsinput and typeahead ------------------------------
    var maxi=$("#maxLimit").val();
    $('#entity-search').tagsinput({
        tagClass: function(item) {//make the coloring of the tags based on classes defined in css
            if ($('ul.typeahead:contains("'+$("input.entity").val()+'")').length > 0) {//check to see if the text contains in the ul otherwise it is just a term

                //based on the type return a class to be added to  the label
                //each class has its own coloring and can be used to figure out the type later
                var type = $("input.entity").attr('type-value');
                if (type == "LOC") {
                    return "label label-loc";
                }
                else if (type == "PERS") {
                    return "label label-act";
                }
                else if (type == "DAT") {
                    return "label label-dat";
                }
                else if (type == "ORG") {
                    return "label label-org";
                }
            }
            else {
                return "label label-term";
            }

        },
        itemID: function(item) {
            if ($('ul.typeahead:contains("'+$("input.entity").val()+'")').length > 0) {//check to see if the text contains in the ul otherwise it is just a term
                return $("input.entity").attr('node_id');//if there is entity it has a node_it ,set it as itemID
            }
        },maxTags: maxi,//maximum number of tag possible
    });

//        taginput search box
    $('.entity').typeahead({
        highlighter: function(item){//returns the data that is then populated as an li in the dropdown
            var img;//set the icon based on type
            if(item.type=="PERS"){
                img='js/img/person.png';
            }else if(item.type=="DAT"){
                img='js/img/date.png';
            }else if(item.type=="ORG"){
                img='js/img/org.png';
            }else if(item.type=="LOC"){
                img='js/img/place.png';
            }
            //the box that contains a term in the dropdownlist
            //to change how terms are displayed this part should be edited
            var html = '<div class="typeahead" style="color:#223447;">';
            html += '<div class="media"><a class="pull-left" href="#"><img src='+img+' /></a>'
            html += '<div class="media-body"> <input type="hidden" class="type-value" value='+item.type+'><input type="hidden" class="node_id_value" value='+item.node_id+'>';

            if(item.wikidata_id==undefined)//if it is a date
            {
                html += '<p class="media-heading"><div class="ll" >'+item.label+'</div><br>';//dates have no wiki_id
            }
            else
            {

                    html += '<p class="media-heading"><div class="ll" >' + item.label + "</div><i style='font-size:small'> &nbsp;&nbsp; (" + item.wikidata_id + ")</i>" + '<br>';

            }

            if(item.description!=undefined)
            {
            html += '<div style="font-size:small">'+item.description+'</div></p></div>';}
            html += '</div>';
            return html;
        },
        ajax: {//send ajax requst
            //to find the top matching terms in the database and fill the typeahead dropdownlist
            url: '/search',//route to look for
            method: "post",
            triggerLength: 1,
            displayField: "label",//field that is used to show as text
            valueField: 'id',
            preDispatch: function (query) {//before the ajax request is send
                return {
                    search: query
                }
            },preProcess: function (resultSet) {//before the data is dispalyed
                    if(resultSet.messages)//if there are any error messages show them to user
                    {
                        BootstrapDialog.show({
                            type: BootstrapDialog.TYPE_DANGER,
                            title: 'Error',
                            message: resultSet.messages,});
                    }
                    return resultSet.result;
            }
        },scrollBar: true//so that after a maximum lenght is reached in the dropdown it will become a scrollbar
    });
//-------------------------------Search for the Term or Entity ------------------------------
    $("a.search-btn-group").click(function(e){

        var tags=$("#entity-search").tagsinput('items');//returns the items that are taged
        //validate the field
        if(tags.size==0 && $("input.entity").val().length>0)//if there is something typed but enter is not pressed
        {
            //make enter happen so the user doesnt change it and a tag happen otherwise the tag list will be empty
            $(function() {
                var e = $.Event('keypress');
                e.which = 13; // Character 'A'
                $('input.entity').trigger(e);
            });
        }
        else if(tags==null || tags.size==0 )//if nothing is typed and the list is empty
        {
            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: 'Error',
                message: 'Please Provide a search Term!',});
            e.preventDefault();
            return;
        }

        //make a querystring to be send to the server .
        var q='';
        function parseMapElements(value, key, map) {
            q=q+value.node_id+"!!"+value.type+"!!"+key+'$';
        }
        $("#entity-search").tagsinput("items").forEach(parseMapElements);// it returns a map with keys as the terms and values as the types
        var target=$(this).attr("data-value");//get the target type of the query
        //send request to server
       $(this).attr("href","/findmatch?query="+q+"&&target="+target+"&&user="+fp1.get());//change the href of the button to contain the query string
    });

    //-------------------------------getting started ------------------------------
    $("#getting_started").click(function(e){
            BootstrapDialog.show({//show the getting started page in a modal
                type: BootstrapDialog.TYPE_INFO,
                title: 'Getting Started',
                message: $('<div></div>').load('getting_started'),});
            e.preventDefault();
    });

    //-------------------------------FAQ ------------------------------
    $("#faq").click(function(e){
        BootstrapDialog.show({//show the FAQ page in a modal
            type: BootstrapDialog.TYPE_INFO,
            title: 'FAQ',
            message: $('<div></div>').load('faq'),});
        e.preventDefault();
    });
    //-------------------------------Create Graph ------------------------------
    $("#btn_graph").click(function(e){

        var tags=$("#entity-search").tagsinput('items');//returns the items that are taged
        //validate the field
        if(tags.size==0 && $("input.entity").val().length>0)//if there is something typed but enter is not pressed
        {
            //make enter happen so the user doesnt change it and a tag happen otherwise the tag list will be empty
            $(function() {
                var e = $.Event('keypress');
                e.which = 13; // Character 'A'
                $('input.entity').trigger(e);
            });
        }
        else if(tags==null || tags.size==0 )//if nothing is typed and the list is empty
        {
            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: 'Error',
                message: 'Please Provide a search Term!',});
            e.preventDefault();
            return;
        }

        //send query params
        q='';
        function parseMapElements(value, key, map) {
            q=q+value.node_id+"!!"+value.type+"!!"+key+'$';
        }
        $("#entity-search").tagsinput("items").forEach(parseMapElements);// it returns a map with keys as the terms and values as the types
        //send request to server

        $("#btn_graph").attr("href","/findgraph?query="+q+"&&user="+fp1.get());//change the href attribute of the a to contain the query strings

    });

    //------Topics
    $("#btn_topic").click(function(e){

        //toggle the div to show the futhur options
            $('#collapse').collapse('toggle');
            e.preventDefault();
            $('.invsi').toggle();
            $("#second_topic_btn").toggle();
            if($('#topic_btn_b').html()==" Topic <img width=\"24px\" src=\"js/img/topic.png\">" )
            {
                $('#topic_btn_b').html("<img width=\"25px\" src=\"js/img/back.png\">");
                $('#topic_btn_b').attr({"data-original-title":"back"
                ,"title":"Back",
                "before":"withImage"});
            }
            else if ($('#topic_btn_b').html()=="<img width=\"25px\" src=\"js/img/back.png\">")
            {
                if ($('#topic_btn_b').attr("before")=="withImage") {
                    $("#topic_btn_b").html(" Topic <img width=\"24px\" src=\"js/img/topic.png\">");
                    $('#topic_btn_b').attr({
                        "data-original-title": "Search for Topics"
                        , "title": "Search for Topics"
                    });
                }
                else {
                    $("#topic_btn_b").html("<img width=\"25px\" src=\"js/img/topic.png\">");
                    $('#topic_btn_b').attr({
                        "data-original-title": "Search for Topics"
                        , "title": "Search for Topics"
                    });
                }

            }
            else if( $('#topic_btn_b').html()=="<img width=\"25px\" src=\"js/img/topic.png\">")
            {
                $('#topic_btn_b').html("<img width=\"25px\" src=\"js/img/back.png\">");
                $('#topic_btn_b').attr({"data-original-title":"back"
                    ,"title":"Back",
                    "before":"noImage"});
            }

            return;
    });


    //------Topics final to submit the form
    $("#btn_topic_final").click(function(e){

        var tags=$("#entity-search").tagsinput('items');//returns the items that are taged
        //validate the field
        if(tags.size==0 && $("input.entity").val().length>0)//if there is something typed but enter is not pressed
        {
            //make enter happen so the user doesnt change it and a tag happen otherwise the tag list will be empty
            $(function() {
                var e = $.Event('keypress');
                e.which = 13; // Character 'A'
                $('input.entity').trigger(e);
            });
        }
        else if(tags.size<2 || tags.size==0 )//less that 2 terms
        {
            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: 'Error',
                message: 'Please Provide at least 2 inputs for Topic generation!',});
            e.preventDefault();
            return;
        }
        //send query params
        q='';
        function parseMapElements(value, key, map) {
            q=q+value.node_id+"!!"+value.type+"!!"+key+'$';
        }
        $("#entity-search").tagsinput("items").forEach(parseMapElements);// it returns a map with keys as the terms and values as the types

        //send request to server
        $("#btn_topic_final").attr("href","/findtopic?query="+q+"&&user="+fp1.get()+"&&toDate="+$("#toDate").val()+"&&fromDate="+$("#fromDate").val()+"&&edges="+$("#num_edges").val()+"&&terms="+$("#num_terms").val());//change the href attribute of the a to contain the query strings

    });

    //------------make the placeholder hidden when a key is pressed

    $(".entity").keydown(function(e)
    {
        $(this).attr("placeholder","");//when someone presses a key in the textbox the placeholder should disappear
    });
    //---------------make tooltip available

    $('[data-toggle="tooltip"]').tooltip();

    // //-----------entity inpute on key down
    // $("input.entity").on('keydown',function(e){
    //     if(e.keycode==13){
    //         if($("ul > li.active").length<1) {
    //             $("#entity-search").tagsinput('add', {
    //                 "label": $("input.entity").val(),
    //                 "type": "TER",
    //                 "outside": true
    //             });
    //     }
    //
    // }});

});