function graphStart(file)
{
    drawGraph(file);
    var network,edges,nodes,nodesArray, edgesArray;
    function drawGraph( json) {
        json = JSON.parse( json );//parse the data into json
        nodesArray=json.nodes;
        edgesArray=json.edges;


        var  width = window.innerWidth,
            height = window.innerHeight-100;//to define the size of the graph window
        minWidth=0.2,maxWidth=7;//for the links to scale between them
        //scale for the link width
        var scale= d3.scale.linear()
            .domain([0, 1])
            .range([minWidth, maxWidth]);

        //read the edges and adjust the width
        $.each(edgesArray, function (index, value) {
            value['width']=scale(value.weight);
            value['to']=value['target'];
            value['from']=value['source'];
            value['color']={color:'lightgrey'};
        });
        //read all the nodes and add the photos with the description for tooltip as title
        $.each(nodesArray, function (index, value) {
            nodesArray[index].id=value['uniqueKey'];
            nodesArray[index].size=16;
            nodesArray[index].title=value.type+"-"+value.description;
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
        });
        //so the container fills the scroon
        $('#graph-container').height(height).width(width);
        var container = document.getElementById('mynetwork');

        var options = {
            nodes: {
                shape: 'image',//have to be set in case of images
                font: {
                    face: 'Tahoma'
                }
            },
            edges: {
                smooth: {
                    type: 'continuous'
                    ,roundness: 0//so the edges are staight
                }
            },
            interaction: {
                tooltipDelay: 200,
                hideEdgesOnDrag: true,
                zoomView:false,//so we dont zoom
                hover: true// otherwise the tooltip will nont work
            },
            physics: {
                stabilization: false,
                barnesHut: {
                    gravitationalConstant: -8000,
                    springConstant: 0.01,
                    springLength: 150
                },
                minVelocity: 0.75//a little movement at the begining
            }

        };
        nodes=new vis.DataSet(nodesArray);
        edges=new vis.DataSet(edgesArray);
        var data = {
            nodes: nodes,
            edges: edges
        };
        //start the graph
        network = new vis.Network(container, data, options);

    };


}
