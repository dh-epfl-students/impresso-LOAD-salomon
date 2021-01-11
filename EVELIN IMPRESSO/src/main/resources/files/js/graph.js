function graphStart(file)
{
    var  width = window.innerWidth,
        height = window.innerHeight
    minWidth=0.2,maxWidth=7;//for the links to scale between them

    //tootip
    var tooltip=d3.select('body')
        .append('div')
        .style('position','absolute')
        .style('padding','0 10px')
        .style('background','#F4F3EE')
        .style('opacity',0);

    //scale for the link width
    var scale= d3.scale.linear()
        .domain([0, 1])
        .range([minWidth, maxWidth]);

    //svg canvas
    var svg = d3.select("svg");
    drawGraph(file);
   function drawGraph( json) {
       json = JSON.parse( json );//parse the data into json

        var node_drag = d3.behavior.drag()
            .on("dragstart", dragstart)
            .on("drag", dragmove)
            .on("dragend", dragend);

        function dragstart(d, i) {
            force.stop() // stops the force auto positioning before you start dragging
        }

        function dragmove(d, i) {
            d.px += d3.event.dx;
            d.py += d3.event.dy;
            d.x += d3.event.dx;
            d.y += d3.event.dy;
            tick(); // this is the key to make it work together with updating both px,py,x,y on d !
        }

        function dragend(d, i) {
            d.fixed = true; // of course set the node to fixed so the force doesn't include the node in its auto positioning stuff
            tick();
            force.resume();
            force.stop();//NEW
        }
        var k = Math.sqrt(json.nodes.length / (width * height));
        var force = d3.layout.force()
            .gravity(80 * k)
            .charge(-20 / k)
            .linkDistance(170)
            .size([width, height]);
        force
            .nodes(json.nodes)//add nodes to force layout
            .links(json.edges)//add edges to froce layout
            .start();

        var link = svg.selectAll(".link")
            .data(json.edges)
            .enter().append("line")
            .attr("class", "link")
            .attr("stroke-width", function(d) { return ( scale(d.weight)); });

        var node = svg.selectAll(".node")
            .data(json.nodes)//put the data into nodes
            .enter().append("g")
            .attr("class", "node")
            .call(node_drag);

        //add images to the nodes
        node.append("image")
            .attr("xlink:href", function(d,i){
                var color="normal";
                if(d.isQueryEntity)
                {
                    color="red";//red means whatever is of intereset at the moment
                    //all the red photos have a box around them
                    //so if it is the entity we searched for make a box around it
                }
                if(d.type=="PERS")
                {
                    return "js/img/person-"+color+".png";
                }
                else if(d.type=="TER")
                {
                    return "js/img/term-"+color+".png";
                }
                else if(d.type=="ORG")
                {
                    return "js/img/org-"+color+".png";
                }
                else if(d.type=="DAT")
                {
                    return "js/img/date-"+color+".png";
                }
                else if(d.type=="PAG")
                {
                    return "js/img/page-"+color+".png";
                }
                else if(d.type=="LOC")
                {
                    return "js/img/place-"+color+".png"
                }
            })
            .attr("x", -15)
            .attr("y", -15)
            .attr("width", 27)
            .attr("height", 27);

        // add the text next to nodes
        node.append("text")
            .attr('fill',function(d,i){
                    return "#2c3e50";
            })
            .attr('font-size',function(d,i){
                if(d.isQueryEntity)
                {
                    return '18px';//if it is what we searched for make it a bit bigger
                }
                else
                {
                    return '16px';
                }
            })
            .attr("dx", 12)
            .attr("dy", 5)
            .text(function(d) {return d.label });//the text of the nodes are the labels

        // On node hover, examine the links to see if their
// source or target properties match the hovered node.
        node.on('mouseover', function(d) {
            var tool;
            if (d.description==undefined)
                tool=  d.type;
            else
                tool=d.type+"-"+d.description
            tooltip.transition()
                .style('opacity',0.9);
            tooltip.html(tool)//make the tooltip appear
                .style('left',(d3.event.pageX-45)+'px')
                .style('top',(d3.event.pageY-40)+'px');

            link.style('stroke', function(l) {//make the link color change on the mouseover
                if (d === l.source || d === l.target)
                    return "#444444";
                else
                    return "#ccc";
            });
        });
       //
       //  //----- on double click
       node.on('dblclick', function(d) {
           $("#entity-search").tagsinput('add', {"node_id": d.node_id, "label": d.label, "type": d.type, "outside": true}
           );
       });


// Set the stroke width back to normal when mouse leaves the node. and vanish the tooltip
        node.on('mouseout', function() {
            tooltip.style('opacity',0);
            link.style('stroke',"#ccc");
        });
        function tick() {
            link.attr("x1", function(d) { return d.source.x; })
                .attr("y1", function(d) { return d.source.y; })
                .attr("x2", function(d) { return d.target.x; })
                .attr("y2", function(d) { return d.target.y; });

            node[0].x=width/2;
            node[0].y=height/2;
            node.attr("transform",
                function(d,i) {
                    return "translate(" + d.x + "," + d.y + ")"; });

        };
        force.on("tick", tick);
        function resize() {
            width = window.innerWidth, height = window.innerHeight;
            svg.attr("width", width).attr("height", height);
            force.size([width, height]).resume();
            for (var i = 10000; i > 0; --i) force.tick();//NEW
            force.stop();//NEW
        }
        resize();//to make it responsive
        d3.select(window).on("resize", resize);

       for (var i = 1000; i > 0; --i) force.tick();//NEW
       force.stop();//NEW
    };
}


