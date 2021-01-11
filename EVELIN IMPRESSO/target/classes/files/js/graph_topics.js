function graphStart(file)
{
    drawGraph(file);
    var network,edges,nodes,nodesArray, edgesArray;



    //----draw the startup graph
    function drawGraph( json) {
        json = JSON.parse( json );//parse the data into json
        nodesArray=json.nodes;
        edgesArray=json.edges;

        //proccess the json file to shape into desired format
        $.each(edgesArray, ProccessEdges);
        $.each(nodesArray,ProccessNodes);

        var  width = window.innerWidth,
            height = window.innerHeight-100;//to define the size of the graph window
        $('#graph-container').height(height).width(width);
        var container = document.getElementById('mynetwork');

        var options = {
            nodes: {
                shape: 'image',
                font: {
                    face: 'Tahoma'
                }
            },
            edges: {
                smooth: {
                    type: 'continuous'
                    ,roundness: 0
                }
            },
            interaction: {
                tooltipDelay: 200,
                hideEdgesOnDrag: true,
                zoomView:false,
                hover: true
            },
            physics: {
                stabilization: true,
                barnesHut: {
                    gravitationalConstant: -8000,
                    springConstant: 0.01,//smaller closer together
                    springLength: 150
                },
                minVelocity: 0.75
            }

        };
        console.log(nodesArray);
        console.log(edgesArray);
        nodes=new vis.DataSet(nodesArray);
        edges=new vis.DataSet(edgesArray);
        var data = {
            nodes: nodes,
            edges: edges
        };

        var current_id;

        network = new vis.Network(container, data, options);
        network.on("stabilizationIterationsDone", function () {//to stop everything from moving
            network.setOptions( { physics: false } );
        });

        //add event listeners
        network.on("oncontext", function (params) {
            params.event.preventDefault();

            if(this.getNodeAt(params.pointer.DOM)!=undefined) {
                $("ul.list-group").show(100);//show the menu
                $('.sub').css("display","none");//hide submenus
                $(".custom-menu").finish().toggle(100);//make the container visable
                $(".custom-menu").css({
                    left: params.event.offsetX + "px",
                    top: params.event.offsetY + "px"
                });

                current_id=this.getNodeAt(params.pointer.DOM);//save the current node info for later
                current_node=nodesArray[current_id];
                $("#current_node").val(current_node.node_id+"!!"+current_node.type+"!!"+current_node.label);
            }
        });

        //--------- If the document is clicked somewhere
        network.on("click", function (e) {

            $(".add_node").hide(100);
            // If the clicked element is not the menu
            if (!$(e.target).parents(".custom-menu").length > 0) {
                // Hide it
                $(".custom-menu").hide(100);
            }
            if($("#graph").css("cursor")=="copy")//if we are in the add node mode
            {
                $("#graph").css("cursor","auto");//change back the cursor
                var obj = eval('(' +$("#current_add_node").val() + ')');
                obj['x']=e.pointer.canvas.x;//get the position of the cursor to add the new node in that position
                obj['y']= e.pointer.canvas.y;
                var edge= {source: 9, target: 3, weight: 0.0016047011160919573, width: 0.21091196758942532, to: 3,from:9}
                addNode(obj,edge);

            }
        });

        //---Sub drop downs
        $('.dropdown-btn').on("click", function(e){//make the sub drop downs visable
            $('.sub').css("display","none");
            var fp1 = new Fingerprint();
            current_ul=$(this).next('ul');
            $(this).next('ul').toggle();
            e.stopPropagation();
            e.preventDefault();
            type=$(this).children("button").attr("data-value");
            if(type=="RMV")
            {
                removeRandomNode(current_id);
                $(".custom-menu").hide(100);
                return;
            }
            query=$("#current_node").val();
            $.ajax({url:"/find_relevent?query="+query+"&&target="+type+"&&user="+fp1.get(),
                beforeSend: function( xhr ) {
                }}).done(function(data) {
                json = JSON.parse( data );

                if(json.message)//if there are errors show them
                {
                    $("#message").text(json.message);
                    $("#message").removeClass("hidden");
                }
                else {
                    current_ul.html("");
                    $.each(json.results, function( key, value ) {
                        if(type=="PERS")
                        {
                            current_ul.append("<li class=\"li-person\"><span class=\" btn-group dropdown-btn btn-block\"><button data-value=\"{node_id:'"+value.node_id+"',type:'"+value.type+"',label:'"+value.label+"',description:'"+value.description+"'}\" type=\"button\"  class=\"btn_sub btn btn-person \" style=\"border-radius: 0px;padding: 2px;\"  data-placement=\"bottom\" > <span class=\"glyphicon glyphicon-plus\"></span> " +
                                value.label+"</button></span></li >");
                        }
                        else if(type=="LOC")
                        {
                            current_ul.append("<li class=\"li-loc\"><span class=\" btn-group dropdown-btn btn-block\"><button data-value=\"{node_id:'"+value.node_id+"',type:'"+value.type+"',label:'"+value.label+"',description:'"+value.description+"'}\" type=\"button\"  class=\"btn_sub btn btn-loc\" style=\"border-radius: 0px;padding: 2px;\"  data-placement=\"bottom\" > <span class=\"glyphicon glyphicon-plus\"></span> " +
                                value.label+"</button></span></li >");
                        }
                        else if(type=="ORG")
                        {
                            current_ul.append("<li class=\"li-org\"><span class=\" btn-group dropdown-btn btn-block\"><button data-value=\"{node_id:'"+value.node_id+"',type:'"+value.type+"',label:'"+value.label+"',description:'"+value.description+"'}\"  type=\"button\"  class=\"btn_sub btn btn-org\" style=\"border-radius: 0px;padding: 2px;\"  data-placement=\"bottom\" > <span class=\"glyphicon glyphicon-plus\"></span> " +
                                // " </button>" +value.label+"-<i style='font-size: small;'>"+value.description+ "</i></button></span></li >");
                                value.label+"</button></span></li >");
                        }

                    });
                    //--- sub buttons click
                    $(".btn_sub").click(function(e){
                        $('.sub').hide(100);//hide submenus
                        $(".custom-menu").hide(100);
                        $(".add_node").show(100);//make the container visable
                        $(".add_node").css({
                            left: e.pageX-150 + "px",
                            top: e.pageY-150 + "px"
                        });
                        $("#graph").css("cursor","copy");

                        $("#current_add_node").val($(this).attr("data-value"));

                    });
                }

            }).error(function(data){

            }).complete(function(data){
            });
        });

    };

    //---function to proccess each node of the graph before adding to network data
    function ProccessNodes(index, value) {
        nodesArray[index].id=value['uniqueKey'];
        nodesArray[index].size=16;
        nodesArray[index].title=value.type+"-"+value.description;
        // nodesArray[index].chosen= { label: false, node: menu }
        if (value.description==undefined){
            nodesArray[index].title=value.type;
        }

        var color="normal";
        if(value.isQueryEntity)
        {
            nodesArray[index].size=18;
            color="red";//red means whatever is of intereset at the moment
            //all the red photos have a box around them
            //so if it is the entity we searched for make a box around it
        }
        if(value.type=="PERS")
        {
            nodesArray[index].image= "js/img/person-"+color+".png";
        }
        else if(value.type=="TER")
        {
            nodesArray[index].image="js/img/term-"+color+".png";
        }
        else if(value.type=="ORG")
        {
            nodesArray[index].image= "js/img/org-"+color+".png";

        }
        else if(value.type=="DAT")
        {
            nodesArray[index].image= "js/img/date-"+color+".png";
        }
        else if(value.type=="PAG")
        {
            nodesArray[index].image= "js/img/page-"+color+".png";
        }
        else if(value.type=="LOC")
        {
            nodesArray[index].image= "js/img/place-"+color+".png"
        }
    }

    //function to proccess all the edges before adding to network
    function ProccessEdges(index, value) {
        var minWidth=0.2,maxWidth=7;//for the links to scale between them
        //scale for the link width
        var scale= d3.scale.linear()
            .domain([0, 1])
            .range([minWidth, maxWidth]);

        value['width']=scale(value.weight);
        value['to']=value['target'];
        value['from']=value['source'];
        value['color']={color:'lightgrey'};
    }

    //---add a new node to the graph with all the edges
    function addNode(node_info,edge_info) {
        //add a node
        var id=nodesArray[nodesArray.length-1].id;
        node_info.uniqueKey=id+1;
        nodesArray[nodesArray.length]=node_info;
        ProccessNodes(nodesArray.length-1, node_info);
        nodes.add(nodesArray[nodesArray.length-1]);
        //add an edge
        edge_info.source=nodesArray.length;
        edge_info.from=nodesArray.length;
        edgesArray[edgesArray.length]=edge_info;
        ProccessEdges(edgesArray.length, edge_info);
        edges.add(edgesArray[edgesArray.length-1]);
    }

    //---- remove a node based on id
    function removeRandomNode(i) {

        nodes.remove({id:i});
    }


    // function resetAllNodes() {
    //     nodes.clear();
    //     edges.clear();
    //     nodes.add(nodesArray);
    //     edges.add(edgesArray);
    // }
    //
    //
    // function setTheData() {
    //     nodes = new vis.DataSet(nodesArray);
    //     edges = new vis.DataSet(edgesArray);
    //     network.setData({nodes:nodes, edges:edges})
    // }
    //
    // function resetAll() {
    //     if (network !== null) {
    //         network.destroy();
    //         network = null;
    //     }
    //     drawGraph();
    // }


}

