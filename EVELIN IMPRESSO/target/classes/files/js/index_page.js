$(document).ready(function() {
    //---------------make tooltip available
    $('[data-toggle="tooltip"]').tooltip();

    //set the max width of the input field dynamically
    setMaxWidth();
    $( window ).bind( "resize", setMaxWidth ); //Remove this if it's not needed. It will react when window changes size.

    function setMaxWidth() {
        $( ".bootstrap-tagsinput" ).css( "maxWidth", ( $( window ).width() * 0.66  | 0 ) + "px" );
        $(".btn-holder").css( "maxWidth", ( $( window ).width() * 0.66  | 0 ) + "px" );
    }
});