$(document).ready(function () {
  var proxyForm = $('#proxy-form');
  proxyForm.validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      proxy: {
        required: true,
        minlength: 3,
        remote: "/validate"
      }
    },
    messages: {
      email: {
        required: "Please enter a valid email address",
        email: "Please enter a valid email address"
      },
      proxy: {
        required: "Choose an address",
        minlength: jQuery.validator.format("Enter at least {0} characters"),
        remote: jQuery.validator.format("'{0}@forward.cat' is not valid or already in use")
      }
    },
    errorPlacement: function (error, element) {
      error.appendTo(element.parent().find(".help-block"));
    },
    submitHandler: function (form) {
      var userEmail = $('#proxy-form').find('#email').val();
      createCookie("user_email", userEmail, 365);
      form.submit();
    },
    success: function (label) {
      label.remove();
    }
  });

  proxyForm.find('i[rel=\'tooltip\']').popover({
    placement: "top",
    trigger: "manual"
  }).click(function() {
    var currentElem = $(this);
    var isTooltipOpen = hasTooltip(currentElem);
    hideTooltip(getElemsWithTooltip());

    if (!isTooltipOpen) {
      showTooltip(currentElem);
    }
    return false; // Stop propagation & prevent default
  });

  $('body').click(function() {
    hideTooltip(getElemsWithTooltip());  // Hide all tooltips
  });

  var userEmail = readCookie("user_email");
  if (userEmail !== null) {
    proxyForm.find('#email').val(userEmail);
  }

  $('#report-form').validate({
    rules: {
      proxy: {
        required: true,
        minlength: 3,
        email: true
      },
      message: {
        required: true
      }
    },
    messages: {
      email: {
        required: "Please enter a valid proxy address",
        email: "Please enter a valid proxy address"
      },
      message: {
        required: "Required"
      }
    },
    errorPlacement: function (error, element) {
      error.appendTo(element.parent().find(".help-block"));
    },
    submitHandler: function (form) {
      form.submit();
    },
    success: function (label) {
      label.remove();
    }
  });
});

function getElemsWithTooltip() {
  return $('#proxy-form').find("i[rel='tooltip'].tooltip-open");
}

function hasTooltip(elem) {
  return elem.hasClass('tooltip-open');
}

function hideTooltip(elems) {
  elems.removeClass('tooltip-open');
  elems.popover('hide');
}

function showTooltip(elems) {
  elems.popover('show');
  elems.addClass('tooltip-open');
}

function createCookie(name, value, days) {
  var expires = "";
  if (days) {
    var date = new Date();
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
    expires = "; expires=" + date.toGMTString();
  }
  document.cookie = name + "=" + value + expires + "; path=/";
}

function readCookie(name) {
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for (var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) === ' ') c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
  }
  return null;
}
