<#import "/spring.ftl" as s>
<#import "../helpers/header.ftl" as header>

<#macro searchTextAndButton search>
    <div class="row no-margin-bottom valign-wrapper">
        <div class="col s10 m7 offset-m1 l6 offset-l2">
            <div class="input-field">
                <input id="searchText" type="text" name="searchText" value="${search.getSearchText()}">
                <label for="searchText">${locale.getString("search")}</label>
            </div>
        </div>

        <div class="col s2 m3 l4">
            <div class="hide-on-small-only">
                <@header.buttonSubmit name='action' icon='search' localizationKey='search.submit' id='button-save-account'/>
            </div>
            <div class="hide-on-med-and-up">
                <@header.buttonSubmit name='action' icon='search' localizationKey='' id='button-save-account'/>
            </div>
        </div>
    </div>
</#macro>

<#macro checkboxes search>
    <div class="row">
        <div class="col s8 offset-s2 m4 offset-m2 l3 offset-l3">
            <div class="search-checkbox-container">
                <label>
                    <input type="checkbox" name="searchName" <#if search.isSearchName()>checked="checked"</#if>>
                    <span class="text-default">${locale.getString('search.in.name')}</span>
                </label>
            </div>
        </div>
        <div class="col s8 offset-s2 m6 l6">
            <div class="search-checkbox-container">
                <label>
                    <input type="checkbox" name="searchDescription" <#if search.isSearchDescription()>checked="checked"</#if>>
                    <span class="text-default">${locale.getString('search.in.description')}</span>
                </label>
            </div>
        </div>
        <div class="col s8 offset-s2 m4 offset-m2 l3 offset-l3">
            <div class="search-checkbox-container">
                <label>
                    <input type="checkbox" name="searchCategory" <#if search.isSearchCategory()>checked="checked"</#if>>
                    <span class="text-default">${locale.getString('search.in.category')}</span>
                </label>
            </div>
        </div>
        <div class="col s8 offset-s2 m6 l6">
            <div class="search-checkbox-container">
                <label>
                    <input type="checkbox" name="searchTags" <#if search.isSearchTags()>checked="checked"</#if>>
                    <span class="text-default">${locale.getString('search.in.tags')}</span>
                </label>
            </div>
        </div>
        <div class="col s8 offset-s2 m4 offset-m2 l3 offset-l3">
            <div class="search-checkbox-container">
                <label>
                    <input type="checkbox" name="includeHiddenAccounts" <#if search.isIncludeHiddenAccounts()>checked="checked"</#if>>
                    <span class="text-default">${locale.getString('search.include.hidden.accounts')}</span>
                </label>
            </div>
        </div>
    </div>
</#macro>

<#macro pagination page position>
    <div class="row pagination-position-${position}">
        <div class="col s12 center-align">
            <#if page.getTotalPages() gt 0>
                <ul class="pagination">
                    <li class="text-default <#if page.getNumber() == 0>disabled</#if>"><a class="page-link" data-page="${page.getNumber()-1}"><i class="material-icons">chevron_left</i></a></li>
                        <#list 0..page.getTotalPages()-1 as i>
                            <li class="waves-effect text-default <#if page.getNumber() == i>active</#if>"><a class="page-link" data-page="${i}">${i+1}</a></li>
                        </#list>
                    <li class="text-default <#if page.getNumber() == page.getTotalPages()-1>disabled</#if>"><a class="page-link" data-page="${page.getNumber()+1}"><i class="material-icons">chevron_right</i></a></li>
                </ul>
            </#if>
        </div>
    </div>
</#macro>